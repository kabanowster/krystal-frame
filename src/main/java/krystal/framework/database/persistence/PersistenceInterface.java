package krystal.framework.database.persistence;

import krystal.JSON;
import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.database.abstraction.*;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.queryfactory.*;
import krystal.framework.logging.LoggingInterface;
import lombok.val;
import org.json.JSONObject;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO JDoc :)
 */
@FunctionalInterface
public interface PersistenceInterface extends LoggingInterface {
	
	/*
	 * Persistence conversion
	 */
	
	/**
	 * Get all persisted objects from the database of particular type. The class must declare empty (no arguments) constructor. Use {@link QueryExecutorInterface} for initial dependency injection. Use {@link WhereClause#persistenceFilter(Function)} for
	 * filtering the {@link Loader loading} query.
	 *
	 * @param optionalDummyType
	 * 		If provided, will be taken as source for invoked methods in query construction. With, i.e. additional {@link Skip} fields as parameters, you can set up different conditional outputs
	 * 		for key methods, like {@link #getTable()} or {@link #getQuery()}.
	 * @see Loader
	 * @see Reader
	 * @see ReadOnly
	 */
	static <T> Stream<T> streamAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable Function<SelectStatement, WhereClause> filter, @Nullable T optionalDummyType) {
		val query = getQuery(clazz, optionalDummyType);
		return (filter == null ? query : filter.apply(query))
				       .promise(queryExecutor)
				       .compose(qr -> qr.toStreamOf(clazz))
				       .join()
				       .orElse(Stream.empty());
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> Stream<T> streamAll(Class<T> clazz) {
		return streamAll(clazz, QueryExecutorInterface.getInstance(), null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> Stream<T> streamAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return streamAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> Stream<T> streamAll(Class<T> clazz, Function<SelectStatement, WhereClause> filter) {
		return streamAll(clazz, QueryExecutorInterface.getInstance(), filter, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable Function<SelectStatement, WhereClause> filter, @Nullable T optionalDummyType) {
		val query = getQuery(clazz, optionalDummyType);
		return (filter == null ? query : filter.apply(query))
				       .promise(queryExecutor)
				       .compose(qr -> qr.toStreamOf(clazz));
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance(), null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return promiseAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, Function<SelectStatement, WhereClause> filter) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance(), filter, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> CompletableFuture<Stream<T>> futureAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable Function<SelectStatement, WhereClause> filter, @Nullable T optionalDummyType) {
		val query = getQuery(clazz, optionalDummyType);
		return (filter == null ? query : filter.apply(query))
				       .future(queryExecutor)
				       .thenApply(qr -> qr.toStreamOf(clazz).join().orElse(Stream.empty()));
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> CompletableFuture<Stream<T>> futureAll(Class<T> clazz) {
		return futureAll(clazz, QueryExecutorInterface.getInstance(), null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> CompletableFuture<Stream<T>> futureAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return futureAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	static <T> CompletableFuture<Stream<T>> futureAll(Class<T> clazz, Function<SelectStatement, WhereClause> filter) {
		return futureAll(clazz, QueryExecutorInterface.getInstance(), filter, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	@Deprecated
	static <T> Flux<T> fluxAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable Function<SelectStatement, WhereClause> filter, @Nullable T optionalDummyType) {
		val query = getQuery(clazz, optionalDummyType);
		
		return (filter == null ? query : filter.apply(query))
				       .mono(queryExecutor)
				       .map(qr -> qr.toStreamOf(clazz).join().orElse(Stream.empty()))
				       .flatMapMany(Flux::fromStream);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	@Deprecated
	static <T> Flux<T> fluxAll(Class<T> clazz) {
		return fluxAll(clazz, QueryExecutorInterface.getInstance(), null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	@Deprecated
	static <T> Flux<T> fluxAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return fluxAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #streamAll(Class, QueryExecutorInterface, Function, Object)
	 */
	@Deprecated
	static <T> Flux<T> fluxAll(Class<T> clazz, Function<SelectStatement, WhereClause> filter) {
		return fluxAll(clazz, QueryExecutorInterface.getInstance(), filter, null);
	}
	
	/**
	 * Convert rows of {@link QueryResultInterface} data into stream of objects, using their {@link Reader @Reader} annotated constructors. The types of argument of particular constructor must match the data type of each column in the
	 * {@link QueryResultInterface} row. There can be a number of {@link Reader @Readers}, i.e. each responsible for handling different {@link ProviderInterface}.
	 */
	@SuppressWarnings("unchecked")
	static <T> VirtualPromise<Stream<T>> mapQueryResult(QueryResultInterface qr, Class<T> clazz) {
		try {
			val constructor =
					(Constructor<T>) Stream.of(clazz.getDeclaredConstructors())
					                       .filter(c -> c.isAnnotationPresent(Reader.class) && c.trySetAccessible()
							                                    && Arrays.equals(c.getParameterTypes(), qr.columns().values().toArray(Class<?>[]::new)))
					                       .findFirst()
					                       .orElseThrow();
			return VirtualPromise.supply(qr::rows)
			                     .mapFork(Collection::stream, row -> {
				                     try {
					                     return constructor.newInstance(row.values().toArray());
				                     } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
					                     throw new RuntimeException("Exception during QueryResult Persistence Constructor mapping.", e);
				                     }
			                     });
		} catch (NoSuchElementException e) {
			throw new RuntimeException("Exception during QueryResult Persistence: No constructor found.", e);
		}
		
	}
	
	/*
	 * Required setup
	 */
	
	/**
	 * Table linked with object's persistence. Each row of table represents a single object. You can use Lombok to set a {@link lombok.Getter @Getter} marked field - in that case also mark it with {@link Skip @Skip}.
	 * As being a method, you can condition its output on values of other fields that can be {@link Skip Skipped}.
	 *
	 * @see TableInterface
	 */
	TableInterface getTable();
	
	/**
	 * Returns {@link ProviderInterface} used to load instance of an object of provided class.
	 *
	 * @see Provider
	 */
	static <T, D extends T> @Nullable ProviderInterface getProvider(Class<T> clazz, @Nullable D optionalDummyType) {
		return getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(clazz, Provider.class, ProviderInterface.class, optionalDummyType, PersistenceInterface::getProvider);
	}
	
	/**
	 * Declared with {@link Provider @Provider}, or {@link krystal.framework.KrystalFramework#getDefaultProvider() KrystalFramework#getDefaultProvider()} as default.
	 */
	default @Nullable ProviderInterface getProvider() {
		return Tools.getFirstAnnotadedValue(Provider.class, ProviderInterface.class, this);
	}
	
	/**
	 * Returns {@link Query} used to load instance of an object of provided class, default, or throws error if for some reason it's missing.
	 *
	 * @see Loader
	 */
	static <T, D extends T> SelectStatement getQuery(Class<T> clazz, @Nullable D optionalDummyType) {
		return Optional.ofNullable(getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(clazz, Loader.class, SelectStatement.class, optionalDummyType, p -> p.getTable().select()))
		               .orElseThrow();
	}
	
	/**
	 * Returns first custom {@link Loader} query used to load this class or {@code null} if missing.
	 *
	 * @see #getQuery(Class, Object)
	 */
	default @Nullable SelectStatement getQuery() {
		return Tools.getFirstAnnotadedValue(Loader.class, SelectStatement.class, this);
	}
	
	static <T, D extends T, R, A extends Annotation> R getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(Class<T> clazz, Class<A> annotation, Class<R> returnType, @Nullable D optionalDummyType, Function<PersistenceInterface, R> invoker) {
		try {
			T instance;
			if (optionalDummyType != null) {
				instance = optionalDummyType;
			} else {
				Constructor<T> emptyConstructor = clazz.getDeclaredConstructor();
				instance = emptyConstructor.newInstance();
			}
			var result = Tools.getFirstAnnotadedValue(annotation, returnType, instance);
			if (result == null) {
				if (PersistenceInterface.class.isAssignableFrom(clazz)) {
					result = invoker.apply((PersistenceInterface) instance);
				} else {
					throw new RuntimeException("Class %s is not a PersistenceInterface.".formatted(clazz.getSimpleName()));
				}
			}
			return result;
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()));
		}
	}
	
	/**
	 * Mappings of fields names and corresponding columns in database, if other than plain names.
	 */
	private ColumnsMap getFieldsToColumnsMap() {
		return Tools.getFirstAnnotadedValue(ColumnsMapping.class, ColumnsMap.class, this);
	}
	
	/*
	 * Collecting data
	 */
	
	/**
	 * Fields marked as {@link Key @Keys}.
	 */
	private Set<Field> getKeys() {
		return Stream.of(getClass().getDeclaredFields())
		             .filter(f -> f.isAnnotationPresent(Key.class))
		             .collect(Collectors.toSet());
	}
	
	/**
	 * Mapping of fields to database {@link ColumnInterface columns}, including defined in {@link ColumnsMap} if different from fields names.
	 */
	default Map<Field, ColumnInterface> getFieldsColumns() {
		// read overwritten setup
		val m = Optional.ofNullable(getFieldsToColumnsMap())
		                .orElse(ColumnsMap.empty());
		// collect for all fields, either mapping or name
		return Stream.of(getClass().getDeclaredFields())
		             .filter(f -> !f.isAnnotationPresent(Skip.class))
		             .collect(Collectors.toMap(
				             f -> f,
				             f -> Optional.ofNullable(m.columns().get(f)).orElse(f::getName)
		             ));
	}
	
	default Map<Field, Object> getWriters() {
		return Stream.of(getClass().getDeclaredMethods())
		             .filter(m -> m.getName().toLowerCase().matches("^write.*?"))
		             .collect(Collectors.toMap(
				             m -> Arrays.stream(getClass().getDeclaredFields())
				                        .filter(f -> m.getName().toLowerCase().replace("write", "").equalsIgnoreCase(f.getName()))
				                        .findFirst().orElseThrow(),
				             f -> {
					             try {
						             f.setAccessible(true);
						             return Optional.ofNullable(f.invoke(this)).orElse("null");
					             } catch (InvocationTargetException e) {
						             return "null";
					             } catch (IllegalAccessException e) {
						             throw new RuntimeException(e);
					             }
				             }
		             ));
	}
	
	/**
	 * Fields values computed with writers or plain values - if none are set.
	 *
	 * @see Writer @Writer
	 */
	default Map<Field, Object> getFieldsValues() {
		val m = getWriters();
		return Stream.of(getClass().getDeclaredFields())
		             .filter(f -> !f.isAnnotationPresent(Skip.class))
		             .collect(Collectors.toMap(
				             f -> f,
				             f -> Optional.ofNullable(m.get(f)).orElseGet(
						             () -> {
							             try {
								             f.setAccessible(true);
								             return Optional.ofNullable(f.get(this)).orElse("null");
							             } catch (IllegalAccessException e) {
								             throw new RuntimeException(e);
							             }
						             }
				             ),
				             (f1, f2) -> f1,
				             LinkedHashMap::new
		             ));
	}
	
	private ColumnsPairingInterface[] getKeyPairs(Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		return keys.stream()
		           .map(field -> new ColumnIsPair(fieldsColumns.get(field), ColumnOperators.In, List.of(fieldsValues.get(field))))
		           .toArray(ColumnsPairingInterface[]::new);
	}
	
	private boolean keysHaveNoValues(boolean nonIncrementalOnly, Set<Field> keys, Map<Field, Object> fieldsValues) {
		for (Field f : keys.stream().filter(f -> !nonIncrementalOnly || !f.isAnnotationPresent(Incremental.class)).toList())
			if (Optional.ofNullable(fieldsValues.get(f)).isEmpty()) return true;
		return false;
	}
	
	/*
	 * Validation
	 */
	
	/**
	 * Check, if the class specified is valid for persistence writing.
	 */
	static boolean classHasKeys(Class<?> clazz) {
		return Stream.of(clazz.getDeclaredFields()).anyMatch(f -> f.isAnnotationPresent(Key.class));
	}
	
	static boolean classIsReadOnly(Class<?> clazz) {
		return clazz.isAnnotationPresent(ReadOnly.class);
	}
	
	/*
	 * Public overloads
	 */
	
	/**
	 * Loads data from database if record is found.
	 */
	default void load() {
		execute(PersistenceExecutions.load);
	}
	
	/**
	 * Loads data from database or creates a new record if no persistence is found.
	 */
	default void instantiate() {
		execute(PersistenceExecutions.instantiate);
	}
	
	/**
	 * Removes object from database.
	 */
	default void delete() {
		execute(PersistenceExecutions.delete);
	}
	
	/**
	 * Updates the record in database or creates a new one, if missing.
	 */
	default void save() {
		execute(PersistenceExecutions.save);
	}
	
	/**
	 * Only works with {@link Incremental auto-increment} fields present. Persist a copy of the object.
	 */
	default void copyAsNew() {
		// TODO error handling when nulls (empty object)
		execute(PersistenceExecutions.copyAsNew);
	}
	
	/*
	 * Persistence executions
	 */
	
	/**
	 * Factory method for persistence executions. Performs initial checks and data collection.
	 */
	private void execute(PersistenceExecutions execution) {
		log().trace(">>> Performing persistence execution: {}", execution.toString());
		
		if (!classHasKeys(getClass()))
			throw new RuntimeException(String.format("  ! %s.class is missing @Keys - can not perform single persistence operations.", getClass().getSimpleName()));
		
		if (execution != PersistenceExecutions.load && classIsReadOnly(getClass()))
			throw new RuntimeException(String.format("  ! %s.class is marked as @ReadOnly.", getClass().getSimpleName()));
		
		val keys = getKeys();
		val fieldsValues = getFieldsValues();
		
		if (keysHaveNoValues(false, keys, fieldsValues))
			throw new RuntimeException(String.format("  ! Keys for %s.class have no values. Aborting %s.", getClass().getSimpleName(), execution));
		
		val fieldsColumns = getFieldsColumns();
		
		ColumnsPairingInterface[] keysPairs = getKeyPairs(keys, fieldsColumns, fieldsValues);
		
		val table = getTable();
		
		switch (execution) {
			case load -> load(table, keysPairs, fieldsColumns).ifPresentOrElse(this::copyFrom, () -> log().trace(String.format("  ! No record found for persistence to load %s.class.", getClass().getSimpleName())));
			case instantiate -> instantiate(table, keysPairs, fieldsColumns, fieldsValues);
			case delete -> delete(table, keysPairs);
			case save -> save(table, keysPairs, fieldsColumns, fieldsValues);
			case copyAsNew -> copyAsNew(table, keys, fieldsColumns, fieldsValues);
		}
		
	}
	
	/**
	 * Load persistence object from database.
	 */
	private Optional<? extends PersistenceInterface> load(TableInterface table, ColumnsPairingInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns) {
		var query = getQuery();
		if (query == null) query = table.select(fieldsColumns.values().toArray(ColumnInterface[]::new));
		return query.where(keysPairs).setProvider(getProvider()).promise()
		            .compose(qr -> qr.toStreamOf(getClass()))
		            .join()
		            .orElse(Stream.empty())
		            .findFirst();
	}
	
	/**
	 * Load persistence object or create a new record if none found.
	 */
	private void instantiate(TableInterface table, ColumnsPairingInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		load(table, keysPairs, fieldsColumns)
				.ifPresentOrElse(
						this::copyFrom,
						() -> {
							log().trace(String.format("  ! No record found for persistence to load. Creating new %s.class persisted object.", getClass().getSimpleName()));
							insertAndConsumeSelf(table, fieldsColumns, fieldsValues);
						}
				);
	}
	
	/**
	 * Check if object is persisted and update its values, or create a new record.
	 */
	private void save(TableInterface table, ColumnsPairingInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		load(table, keysPairs, fieldsColumns)
				.ifPresentOrElse(
						l -> table.update(fieldsValues.entrySet().stream()
						                              .filter(e -> !e.getKey().isAnnotationPresent(Key.class))
						                              .map(e -> ColumnSetPair.of(fieldsColumns.get(e.getKey()), e.getValue()))
						                              .toArray(ColumnSetPair[]::new))
						          .where(keysPairs)
						          .setProvider(getProvider())
						          .promise()
						          .thenRun(() -> log().trace("    Record updated."))
						          .join(),
						() -> insertAndConsumeSelf(table, fieldsColumns, fieldsValues)
				);
	}
	
	/**
	 * Copy current object as new persisted record. Only if class defines at least one <b>@Incremental @Key</b> field.
	 *
	 * @see Incremental
	 * @see Key
	 */
	private void copyAsNew(TableInterface table, Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		if (keys.stream().noneMatch(f -> f.isAnnotationPresent(Incremental.class)))
			throw new RuntimeException(String.format("  ! %s.class does not have @Incremental keys, thus copying amd saving would result in ambiguity.", getClass().getSimpleName()));
		
		if (keysHaveNoValues(true, keys, fieldsValues))
			throw new RuntimeException(String.format("  ! Obligatory keys have no values. Aborting creation of %s.class.", getClass().getSimpleName()));
		
		insertAndConsumeSelf(table, fieldsColumns, fieldsValues);
	}
	
	/**
	 * Delete persistence record from database.
	 */
	private void delete(TableInterface table, ColumnsPairingInterface[] keysPairs) {
		val deleted = Long.parseLong(String.valueOf(
				table.delete().where(keysPairs).setProvider(getProvider()).promise()
				     .join()
				     .flatMap(QueryResultInterface::getResult)
				     .orElse(0L)));
		if (deleted > 0)
			log().trace("  ! Persisted object deleted from database. Deleted rows: {}", deleted);
		else
			log().trace("  ! Persisted object not deleted from database.");
	}
	
	/*
	 * Technical stuff
	 */
	
	/**
	 * Create persistence record and consume it.
	 */
	private void insertAndConsumeSelf(TableInterface table, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		val value = columnsToValues(fieldsColumns, fieldsValues);
		table.insert()
		     .into(value.keySet().toArray(ColumnInterface[]::new))
		     .values(value.values().toArray())
		     .setProvider(getProvider())
		     .promise()
		     .map(qr -> qr.toStreamOf(getClass()))
		     .join()
		     .ifPresent(this::copyFrom);
	}
	
	private Map<ColumnInterface, Object> columnsToValues(Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		return fieldsColumns.entrySet()
		                    .stream()
		                    .filter(e -> !e.getKey().isAnnotationPresent(Incremental.class))
		                    .collect(Collectors.toMap(
				                    Entry::getValue,
				                    e -> fieldsValues.get(e.getKey()),
				                    (a, b) -> a,
				                    LinkedHashMap::new
		                    ));
	}
	
	/**
	 * Copy fields values based on their names from another object. Passes through visibility restrictions. Any non-matching will be skipped.
	 */
	private <T> void copyFrom(T another) {
		log().trace(String.format("  > Copy fields from object class %s to %s.class...", getClass().getSimpleName(), another.getClass().getSimpleName()));
		
		Stream.of(getClass().getDeclaredFields())
		      .filter(f -> !f.isAnnotationPresent(Skip.class))
		      .forEach(f -> {
			      try {
				      f.setAccessible(true);
				      Field a = another.getClass().getDeclaredField(f.getName());
				      a.setAccessible(true);
				      f.set(this, a.get(another));
			      } catch (IllegalAccessException | NoSuchFieldException e) {
				      log().trace(String.format("    Field %s not found in %s.class. Skipped.", f.getName(), another.getClass().getSimpleName()));
			      }
		      });
		log().trace("    Copy successful.");
	}
	
	/**
	 * Convenient way to return field from this class. Usage: <b><i>this.fld("fieldName")</i></b>.
	 *
	 * @see ColumnsMap
	 */
	default Field fld(String fieldName) {
		return Arrays.stream(getClass().getDeclaredFields()).filter(f -> f.getName().equals(fieldName)).findFirst().orElseThrow();
	}
	
	/**
	 * Type in the format, that will be issued for each field name in a class (using <i>String.format()</i>). Use as return with {@link #getFieldsColumns()}. I.e. <b><i>this.formatAll("[%s]")</i></b> will format all fields as "<i>[fieldName]</i>"
	 */
	default ColumnsMap formatAll(String format) {
		val map = ColumnsMap.define();
		Arrays.stream(getClass().getDeclaredFields()).forEach(f -> map.column(f, () -> format.formatted(f.getName())));
		return map.set();
	}
	
	default JSONObject toJSON() {
		return JSON.from(this);
	}
	
}