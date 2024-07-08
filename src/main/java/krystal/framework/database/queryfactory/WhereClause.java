package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.Query;
import krystal.framework.database.persistence.annotations.Filter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

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
	 * Factory method for {@link Function} used as {@link Filter @Filter}.
	 */
	public static Function<SelectStatement, WhereClause> filter(Function<SelectStatement, WhereClause> where) {
		return where;
	}
	
}