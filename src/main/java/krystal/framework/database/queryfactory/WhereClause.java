package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.implementation.Q;
import lombok.Getter;
import lombok.val;
import org.apache.logging.log4j.util.Strings;

import java.util.*;

@Getter
public class WhereClause extends Query implements OrderByInterface, GroupByInterface {
	
	private final List<WhereClauseOuterBlock> where = Collections.synchronizedList(new LinkedList<>());
	
	public WhereClause(Query query, WhereClauseDelimiter delimiter, ColumnsComparisonInterface... columnsAre) {
		super(query);
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.NULL, new WhereClauseInnerBlock(delimiter, columnsAre)));
	}
	
	public WhereClause orWhere(WhereClauseDelimiter delimiter, ColumnsComparisonInterface... columnsAre) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.OR, new WhereClauseInnerBlock(delimiter, columnsAre)));
		return this;
	}
	
	public WhereClause orWhere(ColumnsComparisonInterface columnIs) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.OR, new WhereClauseInnerBlock(WhereClauseDelimiter.NULL, columnIs)));
		return this;
	}
	
	public WhereClause andWhere(WhereClauseDelimiter delimiter, ColumnsComparisonInterface... columnsAre) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.AND, new WhereClauseInnerBlock(delimiter, columnsAre)));
		return this;
	}
	
	public WhereClause andWhere(ColumnsComparisonInterface columnIs) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.AND, new WhereClauseInnerBlock(WhereClauseDelimiter.NULL, columnIs)));
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		query.append(" WHERE ");
		where.forEach(query::append);
	}
	
	/**
	 * Creates a WHERE filter out of given parameters {@link Map} - columns names and values.
	 * You can include a {@link ComparisonOperator} name within column's string by using {@code $} prefix: {@code column$operator}.
	 *
	 * @apiNote This method intended use is parsing http GET requests. Each value within array of values is being split using comma as delimiter. To escape the split (i.e. if the comma is an intended part of the value), put the value within quotation
	 * marks. Anyway, any surrounding quotation is stripped from the value.
	 */
	public WhereClause filterWith(Map<String, String[]> params) {
		params.forEach((k, v) -> {
			val arg = k.split("\\$", 2);
			var operator = ComparisonOperator.EQUAL;
			try {
				operator = ComparisonOperator.valueOf(arg[1].toUpperCase());
			} catch (IllegalArgumentException | IndexOutOfBoundsException _) {
			}
			val values = switch (operator) {
				case IN, NOT_IN, BETWEEN -> Arrays.stream(v).flatMap(s -> Arrays.stream(s.splitWithDelimiters("'[^']*'|\"[^\"]*\"|[^,]*", 0)))
				                                  .filter(s -> !",".equals(s))
				                                  .map(Tools::dequote)
				                                  .filter(Strings::isNotBlank)
				                                  .toArray();
				default -> v;
			};
			
			this.andWhere(Q.c(arg[0]).is(operator, values));
		});
		return this;
	}
	
}