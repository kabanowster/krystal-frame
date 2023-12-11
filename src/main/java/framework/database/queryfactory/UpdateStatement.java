package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;
import krystal.Tools;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class UpdateStatement extends Query implements WhereClauseInterface {
	
	private final Set<ColumnsPairingInterface> columnSetPairs = Collections.synchronizedSet(new HashSet<>());
	private TableInterface table;
	
	public UpdateStatement(TableInterface table, ColumnsPairingInterface... columnSetPairs) {
		super(QueryType.UPDATE);
		table(table);
		set(columnSetPairs);
	}
	
	public UpdateStatement set(ColumnsPairingInterface... columnSetPairs) {
		this.columnSetPairs.addAll(Stream.of(columnSetPairs).toList());
		return this;
	}
	
	public UpdateStatement table(TableInterface table) {
		this.table = table;
		return this;
	}
	
	@Override
	public UpdateStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build() {
		if (table == null || columnSetPairs.isEmpty())
			return;
		
		//@formatter:off
		query = new StringBuilder(
				String.format(
						"UPDATE %s SET %s",
						table.sqlName(),
						Tools.concat(KrystalFramework.getDefaultDelimeter(), columnSetPairs.stream())
					)
				);
		//@formatter:on
	}
	
	// TODO UPDATE from SELECT. Create "from" chain.
	
}