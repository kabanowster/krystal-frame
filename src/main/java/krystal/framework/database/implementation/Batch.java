package krystal.framework.database.implementation;

import krystal.VirtualPromise;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Singular;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Builder(builderMethodName = "create", buildMethodName = "batch")
public class Batch implements LoggingInterface {
	
	@Singular List<Query> queries;
	
	@Deprecated
	public Flux<QueryResultInterface> flux() {
		return flux(QueryExecutorInterface.getInstance().orElseThrow());
	}
	
	@Deprecated
	public Flux<QueryResultInterface> flux(QueryExecutorInterface queryExecutor) {
		queries.forEach(Query::pack);
		return queryExecutor.executeFlux(queries);
	}
	
	public VirtualPromise<Stream<QueryResultInterface>> promise() {
		return promise(QueryExecutorInterface.getInstance().orElseThrow());
	}
	
	public VirtualPromise<Stream<QueryResultInterface>> promise(QueryExecutorInterface queryExecutor) {
		queries.forEach(Query::pack);
		return VirtualPromise.supply(() -> queryExecutor.execute(queries), "QueryExecutor Batch");
	}
	
	public CompletableFuture<Stream<QueryResultInterface>> future() {
		return future(QueryExecutorInterface.getInstance().orElseThrow());
	}
	
	public CompletableFuture<Stream<QueryResultInterface>> future(QueryExecutorInterface queryExecutor) {
		queries.forEach(Query::pack);
		return VirtualPromise.futureSupply(() -> queryExecutor.execute(queries));
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
		if (clazzes.length != queries.size()) {
			log().fatal("The queries count must be equal with the classes count in the toStreamOf() method of the Batch.");
			throw new RuntimeException();
		}
		
		return promise().map(s -> s.toArray(QueryResultInterface[]::new))
		                .join()
		                .map(qrs -> IntStream.range(0, clazzes.length)
		                                     .boxed()
		                                     .collect(Collectors.toMap(
				                                     i -> clazzes[i],
				                                     i -> {
					                                     try {
						                                     return qrs[i].toStreamOf(clazzes[i]).joinExceptionally().orElseGet(Stream::empty).toList();
					                                     } catch (ExecutionException e) {
						                                     throw new RuntimeException(e);
					                                     }
				                                     }
		                                     )))
		                .orElse(null);
		
	}
	
}