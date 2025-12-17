package krystal.framework.database.abstraction;

import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.KrystalFramework;
import krystal.framework.database.implementation.Q;
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

/**
 * Base for all SQL related operations.
 * Use factory classes accessible from {@link TableInterface}:
 * {@link krystal.framework.database.queryfactory.SelectStatement SelectStatement},
 * {@link krystal.framework.database.queryfactory.UpdateStatement UpdateStatement},
 * {@link krystal.framework.database.queryfactory.InsertStatement InsertStatement},
 * {@link krystal.framework.database.queryfactory.DeleteStatement DeleteStatement},
 * or manually type with {@link Q} factory class.
 */
@Getter
@NoArgsConstructor
public abstract class Query implements QueryExecutionInterface, LoggingInterface {
	
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
	
	public static Stream<Object> parseValuesForSQL(Object... values) {
		return Stream.of(values).map(Query::parseValueForSQL);
	}
	
	public static Object parseValueForSQL(Object value) {
		
		// Enquoted format
		String withSingleQuotes = "'%s'";
		val asString = String.valueOf(value);
		
		if (value == null || asString.equalsIgnoreCase("null")) return "NULL";
		return switch (value) {
			case String val -> String.format(withSingleQuotes, Tools.sanitizeForSql(val));
			case LocalDateTime val -> String.format(withSingleQuotes, Tools.sanitizeForSql(val.format(KrystalFramework.getDatetimeFormat())));
			case LocalDate val -> String.format(withSingleQuotes, Tools.sanitizeForSql(val.format(KrystalFramework.getDateFormat())));
			case Double val -> asString.replace(",", ".");
			case Integer val -> asString;
			case Boolean val -> val ? "1" : "0";
			default ->
				// else
					String.format(withSingleQuotes, Tools.sanitizeForSql(asString));
		};
		
	}
	
	/**
	 * Append stored string with current part's semantics.
	 */
	protected abstract void build(StringBuilder query, Set<String> appendLast);
	
	public Query setProvider(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	/**
	 * This method is used by {@link QueryExecutorInterface} to unify provider for the internal steps of the Loader. To set provider use {@link #setProvider(ProviderInterface)}.
	 */
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
	
	public Query union(boolean all, Query query) {
		return Query.of("""
		                %s
		                UNION%s
		                %s
		                """.formatted(pack().sqlQuery(), all ? " ALL" : "", query.pack().sqlQuery()));
	}
	
	public VirtualPromise<Stream<QueryResultInterface>> promise(QueryExecutorInterface executor) {
		pack();
		return VirtualPromise.supply(() -> executor.execute(List.of(this)), "QueryExecutor");
	}
	
	public VirtualPromise<QueryResultInterface> promise() {
		return promise(QueryExecutorInterface.getInstance().orElseThrow()).map(s -> s.findFirst().orElse(QueryResultInterface.empty()));
	}
	
	public QueryType determineType() {
		QueryType type = getType();
		if (type != null)
			return type;
		
		String query = sqlQuery(); // unpacking!
		for (QueryType t : QueryType.values())
			if (query.matches(String.format("^%s[\\w\\W]*", t.toString()))) return t;
		
		log().warn("  ! UNDEFINED QueryType.");
		return QueryType.UNDEFINED;
	}
	
	// TODO generalize output inserted / deleted
	// TODO test output ins / del with other drivers
}