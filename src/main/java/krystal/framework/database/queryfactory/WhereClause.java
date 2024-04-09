package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.QueryExecutorInterface;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class WhereClause extends Query implements OrderByInterface, GroupByInterface {
	
	/**
	 * Factory method for {@link Function} used in {@link krystal.framework.database.persistence.PersistenceInterface#streamAll(Class, QueryExecutorInterface, Function, Object) PersistenceInterface.streamAll()} and its variants.
	 */
	public static Function<SelectStatement, WhereClause> persistenceFilter(Function<SelectStatement, WhereClause> where) {
		return where;
	}
	
	private final List<WhereClauseOuterBlock> where = Collections.synchronizedList(new LinkedList<>());
	
	public WhereClause(Query query, WhereClauseDelimiter delimeter, ColumnsPairingInterface... columnsAre) {
		super(query);
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.NULL, new WhereClauseInnerBlock(delimeter, columnsAre)));
	}
	
	public WhereClause orWhere(WhereClauseDelimiter delimeter, ColumnsPairingInterface... columnsAre) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.OR, new WhereClauseInnerBlock(delimeter, columnsAre)));
		return this;
	}
	
	public WhereClause orWhere(ColumnIsPair columnIs) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.OR, new WhereClauseInnerBlock(WhereClauseDelimiter.NULL, columnIs)));
		return this;
	}
	
	public WhereClause andWhere(WhereClauseDelimiter delimeter, ColumnIsPair... columnsAre) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.AND, new WhereClauseInnerBlock(delimeter, columnsAre)));
		return this;
	}
	
	public WhereClause andWhere(ColumnIsPair columnIs) {
		where.add(new WhereClauseOuterBlock(WhereClauseDelimiter.AND, new WhereClauseInnerBlock(WhereClauseDelimiter.NULL, columnIs)));
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		query.append(" WHERE ");
		where.forEach(query::append);
	}
	
}