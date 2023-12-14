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
	
	private final Set<ColumnsPairingInterface> columnSetPairs;
	private final TableInterface table;
	
	public UpdateStatement(TableInterface table) {
		super(QueryType.UPDATE);
		columnSetPairs = Collections.synchronizedSet(new HashSet<>());
		this.table = table;
	}
	
	public UpdateStatement(TableInterface table, ColumnsPairingInterface... columnSetPairs) {
		this(table);
		this.columnSetPairs.addAll(Stream.of(columnSetPairs).toList());
	}
	
	public static UpdateStatement table(TableInterface table) {
		return new UpdateStatement(table);
	}
	
	public UpdateStatement set(ColumnsPairingInterface... columnSetPairs) {
		this.columnSetPairs.addAll(Stream.of(columnSetPairs).toList());
		return this;
	}
	
	@Override
	public UpdateStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query) {
		if (table == null || columnSetPairs.isEmpty())
			throw new IllegalArgumentException();
		
		query.append(
				String.format(
						"UPDATE %s SET %s",
						table.sqlName(),
						Tools.concat(KrystalFramework.getDefaultDelimeter(), columnSetPairs.stream())
				)
		);
	}
	
	// TODO UPDATE from SELECT. Create "from" chain.
	
}