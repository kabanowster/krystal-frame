package framework.database.queryfactory;

import framework.KrystalFramework;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.QueryExecutorInterface;
import framework.database.abstraction.QueryInterface;
import framework.database.abstraction.QueryResultInterface;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Getter
public abstract class Query implements QueryInterface {
	
	protected StringBuilder query;
	protected ProviderInterface provider;
	protected QueryType type;
	
	public Query(Query query) {
		this.query = query.getQuery();
		provider = query.getProvider();
		type = query.getType();
	}
	
	public Query(StringBuilder query) {
		this.query = query;
	}
	
	public Query(QueryType type) {
		this.type = type;
	}
	
	public static Query of(String query) {
		return new Query(new StringBuilder(query)) {
			@Override
			protected void build() {
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
		pack();
		return execute(QueryExecutorInterface.getInstance());
	}
	
	public Query pack() {
		build();
		return this;
	}
	
	protected abstract void build();
	
	@Override
	public String toString() {
		return query.toString();
	}
	
	@Override
	public String sqlQuery() {
		return toString();
	}
	
}