package framework.database.queryfactory;

import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;

public class DeleteStatement extends Query implements WhereClauseInterface {
	
	private final TableInterface from;
	
	public DeleteStatement(TableInterface from) {
		super(QueryType.DELETE);
		this.from = from;
	}
	
	public static DeleteStatement from(TableInterface from) {
		return new DeleteStatement(from);
	}
	
	@Override
	public DeleteStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query) {
		if (from == null)
			throw new IllegalArgumentException();
		
		query.append("DELETE FROM ").append(from.sqlName());
	}
	
}