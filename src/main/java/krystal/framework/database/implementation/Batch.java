package krystal.framework.database.implementation;

import krystal.VirtualPromise;
import krystal.framework.database.abstraction.*;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Singular;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Builder(builderMethodName = "create", buildMethodName = "batch")
public class Batch implements QueryExecutionInterface, LoggingInterface {
	
	@Singular protected List<Query> queries;
	
	public VirtualPromise<Stream<QueryResultInterface>> promise(QueryExecutorInterface queryExecutor) {
		queries.forEach(Query::pack);
		return VirtualPromise.supply(() -> queryExecutor.execute(queries), "QueryExecutor Batch");
	}
	
	public VirtualPromise<Stream<QueryResultInterface>> promise() {
		return promise(QueryExecutorInterface.getInstance().orElseThrow());
	}
	
	public Batch setProviders(ProviderInterface provider) {
		queries.forEach(q -> q.setProvider(provider));
		return this;
	}
	
	/**
	 * Executes and maps the Batch to lists of Persisted objects. Keep the order of provided classes with the order of provided queries.
	 *
	 * @see krystal.framework.database.persistence.PersistenceInterface PersistenceInterface
	 */
	public @Nullable Map<? extends Class<?>, ? extends List<?>> toListsOf(Class<?>... clazzes) {
		if (clazzes.length != queries.size())
			throw logFatalAndThrow("The queries count must be equal with the classes count in the toStreamOf() method of the Batch.");
		
		return promise().map(s -> s.toArray(QueryResultInterface[]::new))
		                .joinThrow()
		                .map(qrs -> IntStream.range(0, clazzes.length)
		                                     .boxed()
		                                     .collect(Collectors.toMap(
				                                     i -> clazzes[i],
				                                     i -> qrs[i].toStreamOf(clazzes[i]).joinThrow().orElseGet(Stream::empty).toList()
		                                     )))
		                .orElse(null);
	}
	
}