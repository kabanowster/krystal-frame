package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;

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
	public UpdateStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (table == null || columnSetPairs.isEmpty())
			throw new IllegalArgumentException();
		
		query.append(
				String.format(
						"UPDATE %s SET %s",
						table.getSqlName(),
						Tools.concat(", ", columnSetPairs.stream())
				)
		);
	}
	
	// TODO UPDATE from SELECT. Create "from" chain.
	// TODO output inserted
}