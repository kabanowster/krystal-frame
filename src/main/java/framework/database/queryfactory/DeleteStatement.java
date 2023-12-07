package framework.database.queryfactory;

import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;

public class DeleteStatement extends Query implements WhereClauseInterface {
	
	private TableInterface from;
	
	public DeleteStatement(TableInterface from) {
		super(QueryType.DELETE);
		from(from);
	}
	
	public DeleteStatement from(TableInterface from) {
		this.from = from;
		return this;
	}
	
	@Override
	public DeleteStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build() {
		if (from == null)
			return;
		
		query = new StringBuilder("DELETE FROM " + from.sqlName());
	}
	
}