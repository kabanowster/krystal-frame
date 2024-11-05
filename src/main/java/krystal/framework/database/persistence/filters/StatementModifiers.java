package krystal.framework.database.persistence.filters;

import jakarta.annotation.Nullable;
import krystal.Tools;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.implementation.Q;
import krystal.framework.database.persistence.ColumnsMap;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.queryfactory.ComparisonOperator;
import krystal.framework.database.queryfactory.OrderByDirection;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@Builder(builderMethodName = "define", buildMethodName = "set")
@Getter
public class StatementModifiers {
	
	// private @Singular List<ColumnInterface> columns;
	private UnaryOperator<WhereClause> where;
	private Integer limit;
	private @Singular(value = "orderBy") Map<OrderByDirection, Set<ColumnInterface>> orderBy;
	
	/**
	 * Creates a SQL statement modifiers group, out of given parameters {@link Map} - columns names and values.
	 * You can include a {@link ComparisonOperator} name within column's string by using {@code $} prefix: {@code column$operator=value}.
	 * By default, the column's name for the statement, will be derived from {@link ColumnsMap} if present for provided Class (parameters treated as Class' fields' names), otherwise or if the param is prefixed with {@code _} - the name will be taken as it
	 * is. I.e. {@code _column$operator=value}.
	 * Additionally, you can use special variables settings, prefixing with {@code $}, i.e.: {@code $limit=value}.
	 * Applicable settings (case-insensitive):
	 * <dl>
	 * <dt><strong>$limit=number</strong></dt>
	 * <dd>Equivalent of SQL top-first rows;</dd>
	 * <dt><strong>$orderBy=column_name / $orderBy$direction=column_name</strong></dt>
	 * <dd>Equivalent of SQL ORDER BY clause. {@code direction} can be specified as {@code ASC} (default) or {@code DESC}. Column name is also parsed as field or raw (see above).</dd>
	 * </dl>
	 *
	 * @apiNote This method intended use is parsing http GET requests. Each value within array of values is being split using comma as delimiter. To escape the split (i.e. if the comma is an intended part of the value), put the value within quotation
	 * marks. Anyway, any surrounding quotation is stripped from the value.
	 */
	public static StatementModifiers fromParams(Map<String, String[]> params, @Nullable Class<?> clazz) {
		val namesMap = Optional.ofNullable(clazz)
		                       .map(c -> PersistenceInterface.getFieldsToColumnsMap(c, null))
		                       .map(ColumnsMap::columns)
		                       .map(Map::entrySet)
		                       .stream()
		                       .flatMap(Collection::stream)
		                       .collect(Collectors.toMap(e -> e.getKey().getName(), Entry::getValue));
		
		val modifiers = StatementModifiers.define()
		                                  .where(StatementModifiers.filterWith(params, namesMap));
		
		params.forEach((k, v) -> {
			if (!k.startsWith("$")) return;
			val arg = k.split("\\$", 3);
			
			if ("limit".equalsIgnoreCase(arg[1])) {
				try {
					modifiers.limit(Integer.parseInt(v[0]));
				} catch (NumberFormatException _) {
				}
				return;
			}
			
			if ("orderBy".equalsIgnoreCase(arg[1])) {
				var direction = OrderByDirection.ASC;
				try {
					direction = OrderByDirection.valueOf(arg[2].toUpperCase());
				} catch (IllegalArgumentException | IndexOutOfBoundsException _) {
				}
				val finalDirection = direction;
				
				modifiers.orderBy(
						direction,
						Arrays.stream(v)
						      .map(c -> c.startsWith("_")
						                ? Q.c(c.substring(1))
						                : namesMap.getOrDefault(c, Q.c(c)))
						      .collect(Collectors.toSet()));
				// return;
			}
		});
		
		return modifiers.set();
	}
	
	/**
	 * Creates a {@link WhereClause} statement from given params and values. Primarily used to parse {@code GET} requests filters.
	 * Omits special variables (with {@code $} prefix). Translates each param into {@link ColumnInterface}, either with given {@code namesToColumnsMap} or as it is. If the param starts with {@code _}, then the mapping is bypassed anyway (for that param).
	 * Multiple values (i.e. for {@link ComparisonOperator ComparisonOperator: IN, NOT_IN or BETWEEN}) are built up by splitting with comma. If comma is a part of value, put it within single/double quotes, which in any case, are trimmed.
	 *
	 * @see #fromParams(Map, Class)
	 */
	public static UnaryOperator<WhereClause> filterWith(Map<String, String[]> params, @Nullable Map<String, ColumnInterface> namesToColumnsMap) {
		return (w) -> {
			params.forEach((k, v) -> {
				if (k.startsWith("$")) return;
				
				val arg = k.split("\\$", 2);
				var operator = ComparisonOperator.EQUAL;
				try {
					operator = ComparisonOperator.valueOf(arg[1].toUpperCase());
				} catch (IllegalArgumentException | IndexOutOfBoundsException _) {
				}
				val finalOperator = operator;
				
				val values = switch (finalOperator) {
					case IN, NOT_IN, BETWEEN -> Arrays.stream(v).flatMap(s -> Arrays.stream(s.splitWithDelimiters("'[^']*'|\"[^\"]*\"|[^,]*", 0)))
					                                  .filter(s -> !",".equals(s))
					                                  .map(String::trim)
					                                  .map(Tools::dequote)
					                                  .filter(Strings::isNotBlank)
					                                  .toArray();
					default -> v;
				};
				val column = arg[0].startsWith("_")
				             ? Q.c(arg[0].substring(1))
				             : Optional.ofNullable(namesToColumnsMap)
				                       .map(m -> m.get(arg[0]))
				                       .orElse(() -> arg[0]);
				
				w.andWhere(column.is(finalOperator, values));
			});
			return w;
		};
	}
	
}