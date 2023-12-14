package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
public abstract class Query implements QueryInterface {
	
	protected volatile StringBuilder query = new StringBuilder();
	protected @Setter ProviderInterface provider;
	protected QueryType type;
	protected Set<String> appendLast = new LinkedHashSet<>();
	protected List<Consumer<StringBuilder>> packedBuildSteps = new LinkedList<>();
	
	public Query(Query query) {
		this.query = query.getQuery();
		provider = query.getProvider();
		type = query.getType();
		appendLast = query.getAppendLast();
		packedBuildSteps = query.getPackedBuildSteps();
	}
	
	public Query(QueryType type) {
		this.type = type;
	}
	
	public static Query of(String query) {
		return new Query() {
			@Override
			protected void build(StringBuilder appended) {
				appended.append(query);
			}
		};
	}
	
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
	
	public Query providedBy(ProviderInterface provider) {
		this.provider = provider;
		return this;
	}
	
	// Overloading
	public QueryResultInterface execute() {
		prep();
		return execute(QueryExecutorInterface.getInstance());
	}
	
	/**
	 * Append stored string with current part's semantics.
	 */
	protected abstract void build(StringBuilder query);
	
	public Query pack() {
		packedBuildSteps.addLast(this::build);
		return this;
	}
	
	public Query pack(Consumer<StringBuilder> build) {
		packedBuildSteps.addLast(build);
		return this;
	}
	
	public Query prep() {
		pack();
		append();
		return this;
	}
	
	public void append() {
		appendLast.forEach(a -> packedBuildSteps.addLast(q -> q.append(" ").append(a)));
	}
	
	public Query append(String appender) {
		appendLast.add(appender);
		return this;
	}
	
	public void unpack() {
		packedBuildSteps.forEach(q -> q.accept(query));
		packedBuildSteps.clear();
	}
	
	@Override
	public String toString() {
		unpack();
		return query.toString();
	}
	
	@Override
	public String sqlQuery() {
		return toString();
	}
	
	public TableInterface asTable(String alias) {
		return () -> "(%s) %s".formatted(prep().sqlQuery(), alias);
	}
	
}