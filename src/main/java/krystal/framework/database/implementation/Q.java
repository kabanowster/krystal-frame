package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.TableInterface;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

/**
 * Quick factory for basic {@link Query} abstractions.
 * Use {@code Q.t("name")} to create {@link TableInterface}, {@code Q.c("name")} for {@link ColumnInterface} or {@code Q.q("sql_query")} for quick {@link Query}.
 */
@UtilityClass
public class Q {
	
	public TableInterface t(String name) {
		return () -> name;
	}
	
	public ColumnInterface c(String name) {
		return () -> name;
	}
	
	public Query q(String sql) {
		return Query.of(sql);
	}
	
	public ColumnInterface[] cols(String... names) {
		return Arrays.stream(names)
		             .map(Q::c)
		             .toArray(ColumnInterface[]::new);
	}
	
}