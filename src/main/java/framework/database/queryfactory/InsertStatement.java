package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;
import framework.database.implementation.JDBCDrivers;
import krystal.Tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InsertStatement extends Query {
	
	private final Set<ColumnInterface> columns;
	private final Set<ColumnInterface> output;
	private final List<Object[]> values;
	private final TableInterface into;
	
	public InsertStatement(TableInterface into) {
		super(QueryType.INSERT);
		this.into = into;
		columns = Collections.synchronizedSet(new LinkedHashSet<>());
		output = Collections.synchronizedSet(new LinkedHashSet<>());
		values = Collections.synchronizedList(new LinkedList<>());
	}
	
	public InsertStatement(TableInterface into, ColumnInterface... columns) {
		this(into);
		this.columns.addAll(Stream.of(columns).toList());
	}
	
	public InsertStatement(TableInterface into, Object... values) {
		this(into);
		this.values.add(parseValuesForSQL(values).toArray());
	}
	
	public static InsertStatement into(TableInterface into) {
		return new InsertStatement(into);
	}
	
	public static InsertStatement into(TableInterface into, ColumnInterface... columns) {
		return new InsertStatement(into, columns);
	}
	
	public static InsertStatement into(TableInterface into, Object... values) {
		return new InsertStatement(into, values);
	}
	
	public InsertStatement output(ColumnInterface... columns) {
		output.addAll(Stream.of(columns).toList());
		return this;
	}
	
	/**
	 * Can be chained for multiple rows inserts.
	 */
	public InsertStatement values(Object... values) {
		this.values.add(parseValuesForSQL(values).toArray());
		return this;
	}
	
	@Override
	public InsertStatement providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build() {
		if (values.isEmpty() || into == null)
			return;
		
		query = new StringBuilder(String.format(
				"INSERT INTO %s %s",
				into.sqlName(),
				!columns.isEmpty() ? String.format("(%s)", Tools.concat(KrystalFramework.getDefaultDelimeter(), columns.stream())) : ""
		));
		
		if (JDBCDrivers.sqlserver.equals(provider.jdbcDriver()))
			query.append(String.format(
					" OUTPUT %s",
					output.isEmpty() ? "INSERTED.*" :
					output.stream()
					      .map(c -> "INSERTED." + c.sqlName())
					      .collect(Collectors.joining(KrystalFramework.getDefaultDelimeter()))
			));
		
		query.append(String.format(
				" VALUES %s",
				values.stream()
				      .filter(v -> columns.isEmpty() || v.length == columns.size())
				      .map(v -> String.format("\n    (%s)", Tools.concat(KrystalFramework.getDefaultDelimeter(), Stream.of(v))))
				      .collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())))
		);
		
		if (JDBCDrivers.as400.equals(provider.jdbcDriver()))
			query = new StringBuilder(String.format("SELECT * FROM FINAL TABLE (%s)", query.toString()));
	}
	
}