package krystal.framework.database.implementation;

import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.Builder;
import lombok.Singular;
import lombok.val;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Builder(builderMethodName = "create", buildMethodName = "batch")
public class Batch implements LoggingInterface {
	
	@Singular List<Query> queries;
	
	public Flux<QueryResultInterface> flux() {
		return flux(QueryExecutorInterface.getInstance());
	}
	
	public Flux<QueryResultInterface> flux(QueryExecutorInterface queryExecutor) {
		queries.forEach(Query::pack);
		return queryExecutor.execute(queries);
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
	public Map<Class<?>, List<?>> toListsOf(Class<?>... clazzes) {
		if (clazzes.length != queries.size()) {
			log().fatal("The queries count must be equal with the classes count in the toStreamOf() method of the Batch.");
			throw new RuntimeException();
		}
		
		val qrs = flux().toStream().toArray(QueryResultInterface[]::new);
		
		return IntStream.range(0, clazzes.length).boxed().parallel()
		                .collect(Collectors.toMap(
				                i -> clazzes[i],
				                i -> qrs[i].toStreamOf(clazzes[i]).toList()
		                ));
	}
	
}