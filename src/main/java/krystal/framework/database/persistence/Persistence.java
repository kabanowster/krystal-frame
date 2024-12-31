package krystal.framework.database.persistence;

import krystal.Skip;
import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.database.abstraction.Query;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.persistence.filters.PersistenceFilters;
import krystal.framework.database.persistence.filters.StatementModifiers;
import krystal.framework.database.persistence.filters.ValuesOrder;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * @see #promiseAll(Class)
 * @see #promiseAll(Class, PersistenceFilters)
 * @see #promiseAll(Class, StatementModifiers)
 * @see #promiseAll(Class, long, PersistenceFilters)
 * @see #promiseAll(Class, UnaryOperator)
 * @see #promiseAll(Class, QueryExecutorInterface)
 * @see #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
 */
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
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable StatementModifiers modifiers, @Nullable T optionalDummyType) {
		val isPersistenceClass = PersistenceInterface.class.isAssignableFrom(clazz);
		
		// SELECT
		var select = PersistenceInterface.getSelectQuery(clazz, optionalDummyType);
		
		// LIMIT
		if (modifiers != null) {
			if (modifiers.getLimit() != null && modifiers.getLimit() > 0) select.limit(modifiers.getLimit());
		}
		
		// WHERE
		val filteredQuery = PersistenceInterface.getFilteredQuery(clazz, optionalDummyType).apply(select);
		val modifiedQuery = modifiers == null || modifiers.getWhere() == null ? filteredQuery : modifiers.getWhere().apply(filteredQuery);
		
		// ORDER BY
		Query orderedQuery = modifiers == null || modifiers.getOrderBy().isEmpty()
		                     ? modifiedQuery
		                     : modifiedQuery.orderBy(modifiers.getOrderBy());
		
		return orderedQuery.promise(queryExecutor)
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
	 * Unless {@link Fresh @Fresh} or explicitly set false {@link PersistenceFilters#isMemorized()}, attempts to load {@code atLeast} number of objects from {@link PersistenceMemory}. If the number is not satisfied - loads elements from database with
	 * {@link #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)}.
	 *
	 * @param atLeast
	 * 		If 0, then the method will load only from {@link PersistenceMemory}.
	 * @see Fresh
	 * @see Memorized
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, long atLeast, @NonNull PersistenceFilters filters) {
		if (clazz.isAnnotationPresent(Fresh.class) || !filters.isMemorized()) return promiseAll(clazz, filters.toStatementModifiers(clazz));
		return VirtualPromise.supply(() -> PersistenceMemory.getInstance()
		                                                    .filter(mem -> mem.containsAny(clazz))
		                                                    .map(mem -> mem.find(clazz, filters.toPredicate()))
		                                                    .orElse(List.of()))
		                     .compose(memorized -> {
			                     if (memorized.size() >= (atLeast < 0 ? 0 : atLeast)) {
				                     return VirtualPromise.supply(() -> !filters.getOrderBy().isEmpty() ? ValuesOrder.sort(memorized, filters.getOrderBy(), clazz) : memorized.stream());
			                     } else {
				                     return promiseAll(clazz, filters.toStatementModifiers(clazz));
			                     }
		                     });
	}
	
	/**
	 * @see #promiseAll(Class, long, PersistenceFilters)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, @Nullable PersistenceFilters filter) {
		if (filter == null) return promiseAll(clazz);
		return promiseAll(clazz, 1, filter);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return promiseAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, @Nullable StatementModifiers modifiers) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), modifiers, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
	 */
	public <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, @Nullable UnaryOperator<WhereClause> filter) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), StatementModifiers.define().where(filter).set(), null);
	}
	
}