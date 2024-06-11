package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;

import java.util.Set;

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
	public DeleteStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (from == null)
			throw new IllegalArgumentException();
		
		query.append("DELETE FROM ").append(from.getSqlName());
		
		// TODO output deleted
	}
	
}