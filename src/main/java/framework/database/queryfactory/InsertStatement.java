package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ColumnInterface;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.TableInterface;
import framework.database.implementation.Providers;
import krystal.Tools;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InsertStatement extends Query {
	
	private final Set<ColumnInterface> columns = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<ColumnInterface> output = Collections.synchronizedSet(new LinkedHashSet<>());
	private final List<Object[]> values = Collections.synchronizedList(new LinkedList<>());
	private TableInterface into;
	
	public InsertStatement(TableInterface into, ColumnInterface... columns) {
		this(into);
		columns(columns);
	}
	
	public InsertStatement(TableInterface into) {
		super(QueryType.INSERT);
		into(into);
	}
	
	public InsertStatement(TableInterface into, Object... values) {
		this(into);
		values(values);
	}
	
	public InsertStatement into(TableInterface into) {
		this.into = into;
		return this;
	}
	
	public InsertStatement columns(ColumnInterface... columns) {
		this.columns.addAll(Stream.of(columns).toList());
		return this;
	}
	
	public InsertStatement output(ColumnInterface... columns) {
		output.addAll(Stream.of(columns).toList());
		return this;
	}
	
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
		
		if (Providers.sqlserver.equals(provider))
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
				      .map(v -> String.format("(%s)", Tools.concat(KrystalFramework.getDefaultDelimeter(), Stream.of(v))))
				      .collect(Collectors.joining(KrystalFramework.getDefaultDelimeter())))
		);
		
		if (Providers.as400.equals(provider))
			query = new StringBuilder(String.format("SELECT * FROM FINAL TABLE (%s)", query.toString()));
	}
	
}