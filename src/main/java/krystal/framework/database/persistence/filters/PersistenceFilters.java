package krystal.framework.database.persistence.filters;

import jakarta.annotation.Nullable;
import krystal.Tools;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.implementation.Q;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.annotations.ColumnsMapping;
import krystal.framework.database.queryfactory.ComparisonOperator;
import krystal.framework.database.queryfactory.OrderByDeclaration;
import krystal.framework.database.queryfactory.OrderByDirection;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter
@Log4j2
@Builder(builderMethodName = "define", buildMethodName = "set")
public class PersistenceFilters implements Predicate<Object> {
	
	private @Singular Map<String, ValuesFilter> values;
	private Integer limit;
	private @Singular(value = "orderBy") List<ValuesOrder> orderBy;
	private @Default boolean memorized = true;
	private @Default ConditionalDelimiter valuesFiltersMatchingCondition = ConditionalDelimiter.AND;
	
	/**
	 * Creates a  group, out of given parameters {@link Map} - names and values.
	 * You can include a {@link ComparisonOperator} within name's string by using {@code $} prefix: {@code name$operator=value}.
	 * <p>
	 * Additionally, you can use special variables settings, prefixing with {@code $}, i.e.: {@code $limit=value}.
	 * Applicable settings (case-insensitive):
	 * <dl>
	 * <dt><strong>$limit=number</strong></dt>
	 * <dd>Equivalent of SQL top-first rows;</dd>
	 * <dt><strong>$orderBy=name / $orderBy$direction=name</strong></dt>
	 * <dd>Equivalent of SQL ORDER BY clause. {@code direction} can be specified as {@code ASC} (default) or {@code DESC}.</dd>
	 * </dl>
	 *
	 * @apiNote This method intended use is parsing http GET requests. Each value within array of values is being split using comma as delimiter. To escape the split (i.e. if the comma is an intended part of the value), put the value within quotation
	 * marks. Anyway, any surrounding quotation is stripped from the value.
	 * @see #toStatementModifiers(Class)
	 */
	public static PersistenceFilters fromParams(Map<String, String[]> params) {
		val filters = PersistenceFilters.define();
		
		params.forEach((k, v) -> {
			if (k.startsWith("$")) {
				val arg = k.split("\\$", 3);
				
				if ("limit".equalsIgnoreCase(arg[1])) {
					try {
						filters.limit(Integer.parseInt(v[0]));
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
					
					for (var s : Arrays.stream(v)
					                   .flatMap(s -> Arrays.stream(s.split(",")))
					                   .map(String::trim)
					                   .filter(Strings::isNotBlank)
					                   .toArray(String[]::new))
						filters.orderBy(new ValuesOrder(direction, s));
					return;
				}
				
				if ("memorized".equalsIgnoreCase(arg[1])) {
					try {
						filters.memorized(Boolean.parseBoolean(v[0]));
					} catch (IndexOutOfBoundsException _) {
						filters.memorized(false);
					}
					// return;
				}
			} else {
				val arg = k.split("\\$", 2);
				
				var operator = ComparisonOperator.EQUAL;
				try {
					operator = ComparisonOperator.valueOf(arg[1].toUpperCase());
				} catch (IllegalArgumentException | IndexOutOfBoundsException _) {
				}
				val finalOperator = operator;
				
				val values = switch (finalOperator) {
					case IN, NOT_IN, BETWEEN -> getMultiValues(v);
					default -> v;
				};
				
				filters.value(arg[0], ValuesFilter.are(operator, values));
			}
		});
		
		return filters.set();
	}
	
	private static Object[] getMultiValues(String[] arr) {
		return Arrays.stream(arr).flatMap(s -> Arrays.stream(s.splitWithDelimiters("'[^']*'|\"[^\"]*\"|[^,]*", 0)))
		             .filter(s -> !",".equals(s))
		             .map(String::trim)
		             .map(Tools::dequote)
		             .filter(Strings::isNotBlank)
		             .toArray();
	}
	
	/**
	 * Turn these set of filters into SQL statement filters.
	 * By default, the column's name for the statement, will be derived from provided Class' Class' fields names (including {@link ColumnsMapping}), otherwise or if the param is prefixed with {@code _} - the name will be taken as it is. I.e.
	 * {@code _column$operator=value}.
	 */
	public StatementModifiers toStatementModifiers(@Nullable Class<?> clazz) {
		val modifiers = StatementModifiers.define();
		modifiers.limit(limit);
		
		val namesMap = Optional.ofNullable(clazz).stream()
		                       .map(c -> PersistenceInterface.getFieldsToColumns(c, null))
		                       .map(Map::entrySet)
		                       .flatMap(Collection::stream)
		                       .collect(Collectors.toMap(e -> e.getKey().getName(), Map.Entry::getValue));
		
		modifiers.where(whereClause -> {
			values.forEach((k, v) -> whereClause.andWhere(getColumnForParameter(k, namesMap).is(v.operator(), v.values())));
			return whereClause;
		});
		
		orderBy.forEach(vo -> modifiers.orderBy(new OrderByDeclaration(vo.direction(), getColumnForParameter(vo.name(), namesMap))));
		
		return modifiers.set();
	}
	
	private ColumnInterface getColumnForParameter(String param, Map<String, ColumnInterface> map) {
		return param.startsWith("_") ? Q.c(param.substring(1)) : Optional.ofNullable(map.get(Tools.dequote(param))).orElse(Q.c(param));
	}
	
	@Override
	public boolean test(Object object) {
		val conditionsToMatch = (long) values.size();
		val conditionsMatching = Arrays.stream(object.getClass()
		                                             .getDeclaredFields())
		                               .filter(Field::trySetAccessible)
		                               .filter(f -> values.containsKey(f.getName()))
		                               .filter(f -> {
			                               try {
				                               val value = f.get(object);
				                               return values.get(f.getName()).test(value);
			                               } catch (IllegalAccessException e) {
				                               log.error("PersistenceFilters field access failed", e);
				                               return false;
			                               }
		                               }).count();
		
		switch (valuesFiltersMatchingCondition) {
			case AND -> {
				return conditionsMatching == conditionsToMatch;
			}
			case OR -> {
				return conditionsMatching > 0;
			}
		}
		return false;
	}
	
	public <T> Predicate<T> toPredicate() {
		return this::test;
	}
	
}