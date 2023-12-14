package krystal.framework.database.abstraction;

import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.JDBCDrivers;
import krystal.framework.database.queryfactory.QueryType;
import krystal.framework.logging.LoggingInterface;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
public abstract class Query implements LoggingInterface {
	
	protected volatile StringBuilder query;
	protected ProviderInterface provider;
	protected QueryType type;
	protected List<Query> packedSteps = new LinkedList<>();
	
	public Query(Query query) {
		this.query = query.getQuery();
		provider = query.getProvider();
		type = query.getType();
		packedSteps = query.getPackedSteps();
	}
	
	public Query(QueryType type) {
		this.type = type;
	}
	
	public static Query of(String sql) {
		return new Query() {
			
			@Override
			protected void build(StringBuilder appended, Set<String> appendLast) {
				appended.append(sql);
			}
		};
	}
	
	/**
	 * Append stored string with current part's semantics.
	 */
	protected abstract void build(StringBuilder query, Set<String> appendLast);
	
	public static Stream<Object> parseValuesForSQL(Object... values) {
		return Stream.of(values).map(Query::parseValueForSQL);
	}
	
	public static Object parseValueForSQL(Object value) {
		
		// Enquoted format
		String senq = "'%s'";
		
		if (value == null) return "NULL";
		if (value instanceof String val) return String.format(senq, val);
		if (value instanceof LocalDateTime val) return String.format(senq, val.format(KrystalFramework.getDatetimeFormat()));
		if (value instanceof LocalDate val) return String.format(senq, val.format(KrystalFramework.getDateFormat()));
		if (value instanceof Double val) return String.valueOf(val).replace(",", ".");
		if (value instanceof Integer val) return String.valueOf(val);
		if (value instanceof Boolean val) return val.equals(true) ? "1" : "0";
		
		// else
		return String.format(senq, value);
	}
	
	public Query setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	public void setProvidersPacked(ProviderInterface provider) {
		packedSteps.forEach(q -> q.setProvider(provider));
	}
	
	public Query pack() {
		packedSteps.addLast(this);
		return this;
	}
	
	public String sqlQuery() {
		if (query == null)
			query = new StringBuilder();
		else return query.toString();
		
		val appendLast = new LinkedHashSet<String>();
		packedSteps.forEach(q -> q.build(query, appendLast));
		packedSteps.clear();
		appendLast.forEach(a -> query.append(" ").append(a));
		appendLast.clear();
		
		return query.toString();
	}
	
	public TableInterface asTable(String alias) {
		return () -> "(%s) %s".formatted(pack().sqlQuery(), alias);
	}
	
	public QueryResultInterface execute() {
		return execute(QueryExecutorInterface.getInstance());
	}
	
	public QueryResultInterface execute(QueryExecutorInterface executor) {
		
		pack();
		
		QueryType type = getType();
		if (type == null) type = determineType(); // Optional.Else actually invokes method...
		
		switch (type) {
			case SELECT -> {
				return executor.read(this);
			}
			case INSERT -> {
				// drivers which return inserted rows as result
				if (List.of(
						JDBCDrivers.as400.asProvider(),
						JDBCDrivers.sqlserver.asProvider()
				).contains(getProvider()))
					return executor.read(this);
			}
		}
		
		return QueryResultInterface.singleton(() -> "#", executor.write(this));
	}
	
	private QueryType determineType() {
		String query = sqlQuery(); // unpacking!
		for (QueryType type : QueryType.values())
			if (query.matches(String.format("^%s[\\w\\W]*", type.toString()))) return type;
		
		log().warn("  ! UNDEFINED QueryType.");
		return QueryType.UNDEFINED;
	}
	
}