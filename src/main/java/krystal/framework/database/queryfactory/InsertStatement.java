package krystal.framework.database.queryfactory;

import krystal.Tools;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.DBCDrivers;
import lombok.val;

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
	
	public InsertStatement into(ColumnInterface... columns) {
		this.columns.addAll(Arrays.asList(columns));
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
	public InsertStatement setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	@Override
	public void build(StringBuilder query, Set<String> appendLast) {
		if (values.isEmpty() || into == null)
			throw new IllegalArgumentException();
		
		query.append(String.format(
				"INSERT INTO %s%s",
				into.getSqlName(),
				!columns.isEmpty() ? String.format(" (%s)", Tools.concat(", ", columns.stream().map(ColumnInterface::getSqlName))) : ""
		));
		
		val drv = provider.getDriver();
		if (DBCDrivers.jdbcSQLServer.equals(drv)) {
			query.append(String.format(
					" OUTPUT %s",
					output.isEmpty() ? "INSERTED.*" :
					output.stream()
					      .map(c -> "INSERTED." + c.getSqlName())
					      .collect(Collectors.joining(", "))
			));
		}
		
		query.append(String.format(
				" VALUES %s",
				values.stream()
				      .filter(v -> columns.isEmpty() || v.length == columns.size())
				      .map(v -> String.format("(%s)", Tools.concat(", ", Stream.of(v))))
				      .collect(Collectors.joining(", ")))
		);
		
		if (DBCDrivers.jdbcAS400.equals(provider.getDriver())) {
			query.append(String.format("SELECT * FROM FINAL TABLE (%s)", query));
		}
	}
	
}