package krystal.framework.database.abstraction;

import krystal.VirtualPromise;

import java.util.stream.Stream;

@FunctionalInterface
public interface QueryExecutionInterface {
	
	VirtualPromise<Stream<QueryResultInterface>> promise(QueryExecutorInterface executor);
	
}