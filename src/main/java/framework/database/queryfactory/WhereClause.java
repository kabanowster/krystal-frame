package framework.database.queryfactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class WhereClause extends Query implements OrderByInterface, GroupByInterface {
	
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
	public void build() {
		query.append("\nWHERE ");
		where.forEach(query::append);
	}
	
}