package krystal.framework.database.persistence;

import krystal.Skip;
import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.persistence.filters.Filter;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@UtilityClass
@Log4j2
public class Persistence {
	
	/**
	 * Get all persisted objects from the database of particular type. The class must declare empty (no arguments) constructor. Use {@link QueryExecutorInterface} for initial dependency injection. Use
	 * {@link krystal.framework.database.persistence.annotations.Filter @Filter} for
	 * filtering the {@link Loader loading} query. Utilises {@link VirtualPromise} for fetching the data and parallel mapping of objects.
	 *
	 * @param optionalDummyType
	 * 		If provided, will be taken as source for invoked methods in query construction. With, i.e. additional {@link Skip} fields as parameters, you can set up different conditional outputs
	 * 		for key methods, like {@link PersistenceInterface#getTable()} or {@link PersistenceInterface#getSelectQuery()}.
	 * @see Loader @Loader
	 * @see Reader @Reader
	 * @see ReadOnly @ReadOnly
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable UnaryOperator<WhereClause> filter, @Nullable T optionalDummyType) {
		val query = PersistenceInterface.getFilterQuery(clazz, optionalDummyType).apply(PersistenceInterface.getSelectQuery(clazz, optionalDummyType));
		val finalQuery = filter == null ? query : filter.apply(query);
		val isPersistenceClass = PersistenceInterface.class.isAssignableFrom(clazz);
		
		return finalQuery.promise(queryExecutor)
		                 .map(s -> s.findFirst().orElse(QueryResultInterface.empty()))
		                 .compose(qr -> qr.toStreamOf(clazz))
		                 .map(s -> s.peek(o -> Tools.runAnnotatedMethods(Reader.class, o)))
		                 .map(s -> {
			                 if (!isPersistenceClass || clazz.isAnnotationPresent(Fresh.class)) return s;
			                 return PersistenceMemory.getInstance()
			                                         .map(inMemory -> s.peek(o -> ((PersistenceInterface) o).memorize(inMemory)))
			                                         .orElse(s);
		                 });
	}
	
	/**
	 * Unless {@link Fresh @Fresh}, Attempts to load {@code atLeast} number of objects from {@link PersistenceMemory}. If the number is not satisfied and is class not {@link Memorized} - loads elements from database with
	 * {@link #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)}.
	 *
	 * @see Fresh
	 * @see Memorized
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, long atLeast, Filter filter) {
		return PersistenceMemory.getInstance()
		                        .filter(mem -> !clazz.isAnnotationPresent(Fresh.class))
		                        .filter(mem -> mem.containsAny(clazz))
		                        .map(memory -> memory.find(clazz, filter.toPredicate()))
		                        .filter(list -> list.size() >= (atLeast <= 0 ? 1 : atLeast) || clazz.isAnnotationPresent(Memorized.class))
		                        .map(list -> VirtualPromise.supply(list::stream))
		                        .orElseGet(() -> promiseAll(clazz, filter.toWhereClause(clazz)));
	}
	
	/**
	 * @see #promiseAll(Class, long, Filter)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, Filter filter) {
		return promiseAll(clazz, 1, filter);
	}
	
	/**
	 * Unless {@link Fresh}, attempts to load {@code atLeast} number of objects from {@link PersistenceMemory}.
	 */
	public <T> Optional<List<T>> promiseMemorized(Class<T> clazz, long atLeast, Filter filter) {
		return PersistenceMemory.getInstance()
		                        .filter(mem -> !clazz.isAnnotationPresent(Fresh.class))
		                        .filter(mem -> mem.containsAny(clazz))
		                        .map(mem -> mem.find(clazz, filter.toPredicate()))
		                        .filter(l -> l.size() >= (atLeast <= 0 ? 1 : atLeast));
	}
	
	/**
	 * @see #promiseMemorized(Class, long, Filter)
	 */
	public <T> Optional<List<T>> promiseMemorized(Class<T> clazz, Filter filter) {
		return promiseMemorized(clazz, 1, filter);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return promiseAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, @Nullable UnaryOperator<WhereClause> filter) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), filter, null);
	}
	
}