package krystal.framework.database.persistence;

import krystal.JSON;
import krystal.Skip;
import krystal.Skip.SkipTypes;
import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.database.abstraction.*;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.persistence.annotations.Vertical.PivotColumn;
import krystal.framework.database.persistence.annotations.Vertical.UnpivotToColumns;
import krystal.framework.database.persistence.annotations.Vertical.ValuesColumn;
import krystal.framework.database.queryfactory.*;
import krystal.framework.logging.LoggingInterface;
import lombok.val;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO JDoc :)
 *
 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
 */
@FunctionalInterface
public interface PersistenceInterface extends LoggingInterface {
	
	/*
	 * Persistence conversion
	 */
	
	/**
	 * Get all persisted objects from the database of particular type. The class must declare empty (no arguments) constructor. Use {@link QueryExecutorInterface} for initial dependency injection. Use {@link WhereClause#filter(Function)} for
	 * filtering the {@link Loader loading} query. Utilises {@link VirtualPromise} for fetching the data and parallel mapping of objects.
	 *
	 * @param optionalDummyType
	 * 		If provided, will be taken as source for invoked methods in query construction. With, i.e. additional {@link Skip} fields as parameters, you can set up different conditional outputs
	 * 		for key methods, like {@link #getTable()} or {@link #getSelectQuery()}.
	 * @see Loader @Loader
	 * @see Reader @Reader
	 * @see ReadOnly @ReadOnly
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor, @Nullable UnaryOperator<WhereClause> filter, @Nullable T optionalDummyType) {
		val query = getFilterQuery(clazz, optionalDummyType).apply(getSelectQuery(clazz, optionalDummyType));
		return (filter == null ? query : filter.apply(query))
				       .promise(queryExecutor)
				       .map(s -> s.findFirst().orElse(QueryResultInterface.empty()))
				       .compose(qr -> qr.toStreamOf(clazz));
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		return promiseAll(clazz, queryExecutor, null, null);
	}
	
	/**
	 * @see #promiseAll(Class, QueryExecutorInterface, UnaryOperator, Object)
	 */
	static <T> VirtualPromise<Stream<T>> promiseAll(Class<T> clazz, @Nullable UnaryOperator<WhereClause> filter) {
		return promiseAll(clazz, QueryExecutorInterface.getInstance().orElseThrow(), filter, null);
	}
	
	/**
	 * Convert rows of {@link QueryResultInterface} data into stream of objects, using their {@link Reader @Reader} annotated constructors. The types of argument of particular constructor must match the data type of each column in the
	 * {@link QueryResultInterface} row. There can be a number of {@link Reader @Readers}, i.e. each responsible for handling different {@link ProviderInterface}. The class can be declared as {@link Vertical @Vertical}, in case which, the
	 * {@link QueryResultInterface} will be un-pivoted first.
	 *
	 * @see QueryResultInterface#unpivot(ColumnInterface, ColumnInterface, ColumnInterface...)
	 */
	@SuppressWarnings("unchecked")
	static <T> VirtualPromise<Stream<T>> mapQueryResult(QueryResultInterface qr, Class<T> clazz) {
		
		if (clazz.isAnnotationPresent(Vertical.class)) {
			
			val verticalColumns = getVerticalMandatoryAnnotations(clazz);
			List<? extends ColumnInterface> intoColumns = Optional.ofNullable(Tools.getFirstAnnotatedValue(UnpivotToColumns.class, List.class, clazz, null)).orElse(List.of());
			
			qr.unpivot(verticalColumns.get(PivotColumn.class), verticalColumns.get(ValuesColumn.class), intoColumns.toArray(ColumnInterface[]::new));
			
		}
		
		Constructor<T> constructor;
		try {
			constructor = (Constructor<T>) Stream.of(clazz.getDeclaredConstructors())
			                                     .filter(c -> c.isAnnotationPresent(Reader.class) && c.trySetAccessible()
					                                                  && Arrays.equals(c.getParameterTypes(), qr.columns().values().toArray(Class<?>[]::new)))
			                                     .findFirst()
			                                     .orElseThrow();
		} catch (NoSuchElementException e) {
			throw new RuntimeException("No @Reader constructor found in %s matching the QueryResult columns. Check constructors arguments types with used ProviderInterface.".formatted(clazz.getSimpleName()), e);
		}
		
		return VirtualPromise.supply(qr::rows)
		                     .mapFork(List::stream, row -> {
			                     try {
				                     return constructor.newInstance(row.values().toArray());
			                     } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				                     throw new RuntimeException("Exception during Persistence constructor invocation.\n" + e.getMessage(), e);
			                     }
		                     });
	}
	
	/*
	 * Required setup
	 */
	
	/**
	 * Table linked with object's persistence. Each row of table represents a single object, unless class is {@link Vertical @Vertical}. You can use Lombok to set a {@link lombok.Getter @Getter} marked field - in that case also mark it with
	 * {@link Skip @Skip}.
	 * As being a method, you can condition its output on values of other fields that can be {@link Skip Skipped}.
	 *
	 * @see TableInterface
	 */
	TableInterface getTable();
	
	/**
	 * Returns {@link ProviderInterface} used to load instance of an object of provided class.
	 *
	 * @see Provider @Provider
	 */
	static <T extends PersistenceInterface, D extends T> @Nullable ProviderInterface getProvider(Class<T> clazz, @Nullable D optionalDummyType) {
		return getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(clazz, Provider.class, ProviderInterface.class, optionalDummyType, PersistenceInterface::getProvider);
	}
	
	/**
	 * Declared with {@link Provider @Provider}, or {@link krystal.framework.KrystalFramework#getDefaultProvider() KrystalFramework#getDefaultProvider()} as default.
	 */
	default @Nullable ProviderInterface getProvider() {
		return Tools.getFirstAnnotatedValue(Provider.class, ProviderInterface.class, this);
	}
	
	/**
	 * Returns {@link SelectStatement} used to load instance of an object of provided class, or default, or throws error if for some reason it's missing.
	 *
	 * @see Loader @Loader
	 */
	static <T> SelectStatement getSelectQuery(Class<? extends T> clazz, @Nullable T optionalDummyType) {
		
		try {
			T instance;
			if (optionalDummyType != null) {
				instance = optionalDummyType;
			} else {
				Constructor<? extends T> emptyConstructor = clazz.getDeclaredConstructor();
				instance = emptyConstructor.newInstance();
			}
			
			return Optional.ofNullable(Tools.getFirstAnnotatedValue(Loader.class, SelectStatement.class, instance))
			               .orElseGet(() -> {
				               if (PersistenceInterface.class.isAssignableFrom(clazz)) {
					               val obj = (PersistenceInterface) instance;
					               return Arrays.stream(clazz.getDeclaredClasses())
					                            .filter(c -> c.isAnnotationPresent(Loader.class) && ColumnInterface.class.isAssignableFrom(c) && Enum.class.isAssignableFrom(c))
					                            .findFirst()
					                            .map(c -> obj.getTable().select((ColumnInterface[]) c.getEnumConstants()))
					                            .orElseGet(() -> obj.getTable().select());
				               } else {
					               throw new RuntimeException("Class %s is not a PersistenceInterface nor declares single @Loader SelectStatement returning method.");
				               }
			               });
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()));
		}
	}
	
	/**
	 * Returns first custom {@link Loader @Loader} query used to load this class or {@code null} if missing.
	 *
	 * @see #getSelectQuery(Class, Object)
	 */
	default @Nullable SelectStatement getSelectQuery() {
		return getSelectQuery(getClass(), this);
	}
	
	@SuppressWarnings("unchecked")
	static <T> Function<SelectStatement, WhereClause> getFilterQuery(Class<? extends T> clazz, @Nullable T optionalDummyType) {
		try {
			T instance;
			if (optionalDummyType != null) {
				instance = optionalDummyType;
			} else {
				Constructor<? extends T> emptyConstructor = clazz.getDeclaredConstructor();
				instance = emptyConstructor.newInstance();
			}
			
			return Arrays.stream(clazz.getDeclaredMethods())
			             .filter(m -> {
				             val primarily = m.trySetAccessible() && m.isAnnotationPresent(Filter.class) && Function.class.isAssignableFrom(m.getReturnType());
				             if (!primarily) return false;
				             val params = Set.of(Tools.determineParameterTypes(m.getGenericReturnType()).types());
				             return params.size() == 2 && params.containsAll(Set.of(SelectStatement.class, WhereClause.class));
			             })
			             .findFirst()
			             .map(m -> {
				             try {
					             return (Function<SelectStatement, WhereClause>) m.invoke(instance);
				             } catch (IllegalAccessException | InvocationTargetException e) {
					             throw new RuntimeException(e);
				             }
			             })
			             .orElse(s -> s.where1is1());
			
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()));
		}
	}
	
	default Function<SelectStatement, WhereClause> getFilterQuery() {
		return getFilterQuery(getClass(), this);
	}
	
	static <T extends PersistenceInterface, D extends T, R, A extends Annotation> R getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(Class<T> clazz, Class<A> annotation, Class<R> returnType, @Nullable D optionalDummyType,
	                                                                                                                                       Function<PersistenceInterface, R> invoker) {
		try {
			T instance;
			if (optionalDummyType != null) {
				instance = optionalDummyType;
			} else {
				Constructor<T> emptyConstructor = clazz.getDeclaredConstructor();
				instance = emptyConstructor.newInstance();
			}
			var result = Tools.getFirstAnnotatedValue(annotation, returnType, instance);
			if (result == null) {
				if (PersistenceInterface.class.isAssignableFrom(clazz)) {
					result = invoker.apply(instance);
				} else {
					throw new RuntimeException("Class %s is not a PersistenceInterface.".formatted(clazz.getSimpleName()));
				}
			}
			return result;
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw LoggingInterface.logFatalAndThrow(LoggingInterface.logger(), "Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()));
		}
	}
	
	/**
	 * Mappings of fields names and corresponding {@link ColumnInterface columns} in database, if other than plain names.
	 *
	 * @see ColumnsMap
	 * @see ColumnsMapping @ColumnsMapping
	 */
	@SuppressWarnings("unchecked")
	default <E extends Enum<?> & ColumnInterface> ColumnsMap getFieldsToColumnsMap() {
		val clazz = getClass();
		return Arrays.stream(clazz.getDeclaredClasses())
		             .filter(c -> c.isAnnotationPresent(ColumnsMapping.class) && ColumnInterface.class.isAssignableFrom(c) && Enum.class.isAssignableFrom(c))
		             .findFirst()
		             .map(c -> ColumnsMap.fromColumnInterfaceEnum(clazz, (Class<E>) c))
		             .orElseGet(() -> Tools.getFirstAnnotatedValue(ColumnsMapping.class, ColumnsMap.class, this));
	}
	
	/*
	 * Collecting data
	 */
	
	/**
	 * Fields marked as {@link Key @Keys}.
	 */
	default Set<Field> getKeys() {
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
		             .filter(f -> !Tools.isSkipped(f, SkipTypes.persistence))
		             .collect(Collectors.toMap(
				             f -> f,
				             f -> Optional.ofNullable(m.columns().get(f)).orElse(f::getName)
		             ));
	}
	
	default Map<Field, Object> getWriters() {
		return Stream.of(getClass().getDeclaredMethods())
		             .filter(m -> m.trySetAccessible() && (
				             m.getName().toLowerCase().matches("^write.*?")
						             || m.isAnnotationPresent(Writer.class)))
		             .collect(Collectors.toMap(
				             m -> Arrays.stream(getClass().getDeclaredFields())
				                        .filter(f -> {
					                                val name = f.getName();
					                                return m.getName().toLowerCase().replace("write", "").equalsIgnoreCase(name)
							                                       || Optional.ofNullable(m.getAnnotation(Writer.class)).map(a -> a.fieldName().equalsIgnoreCase(name)).orElse(false);
				                                }
				                        )
				                        .findFirst().orElseThrow(() -> {
							             val exception = new NoSuchElementException("PersistenceInterface: Could not find field corresponding to writer method (case-insensitive): %s.".formatted(m.getName()));
							             log().error(exception.getMessage());
							             return exception;
						             }),
				             m -> {
					             try {
						             return Optional.ofNullable(m.invoke(this)).orElse("null");
					             } catch (InvocationTargetException e) {
						             return "null";
					             } catch (IllegalAccessException e) {
						             throw logFatalAndThrow(e.getMessage());
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
		             .filter(f -> !Tools.isSkipped(f, SkipTypes.persistence))
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
				             (f, _) -> f,
				             LinkedHashMap::new
		             ));
	}
	
	private ColumnsComparisonInterface[] getKeyValuePairs(Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		val includeIfNull = Optional.ofNullable(getClass().getAnnotation(SeparateKeys.class)).map(SeparateKeys::includeIfNull).orElse(true);
		return keys.stream()
		           .map(field -> new ColumnToValueComparison(fieldsColumns.get(field), ColumnsComparisonOperator.IN, List.of(fieldsValues.get(field))))
		           .filter(cvc -> cvc.values().isEmpty() ? includeIfNull : true)
		           .toArray(ColumnsComparisonInterface[]::new);
	}
	
	private boolean keysAreMissingValues(boolean nonIncrementalOnly, Set<Field> keys, Map<Field, Object> fieldsValues) {
		val finalKeys = keys.stream().filter(f -> !nonIncrementalOnly || !f.isAnnotationPresent(Incremental.class)).toList();
		int i = finalKeys.size();
		for (Field f : finalKeys)
			if (Optional.ofNullable(fieldsValues.get(f)).isEmpty()) --i;
		return getClass().isAnnotationPresent(SeparateKeys.class) ? i == 0 : i != finalKeys.size();
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
			throw logFatalAndThrow(String.format("%s.class is missing @Keys - can not perform single persistence operations.", getClass().getSimpleName()));
		
		if (execution != PersistenceExecutions.load && classIsReadOnly(getClass()))
			throw logFatalAndThrow(String.format("%s.class is marked as @ReadOnly.", getClass().getSimpleName()));
		
		val keys = getKeys();
		val fieldsValues = getFieldsValues();
		
		if (keysAreMissingValues(false, keys, fieldsValues))
			throw logFatalAndThrow(String.format("Keys for %s.class have no values. Aborting %s.", getClass().getSimpleName(), execution));
		
		val fieldsColumns = getFieldsColumns();
		
		ColumnsComparisonInterface[] keysPairs = getKeyValuePairs(keys, fieldsColumns, fieldsValues);
		
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
	private Optional<? extends PersistenceInterface> load(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns) {
		var query = getSelectQuery();
		if (query == null) {
			val clazz = getClass();
			ColumnInterface[] queryColumns;
			if (clazz.isAnnotationPresent(Vertical.class)) {
				
				val verticalColumns = getVerticalMandatoryAnnotations(clazz);
				
				List<ColumnInterface> columns = fieldsColumns.entrySet().stream()
				                                             .collect(Collectors.partitioningBy(e -> e.getValue().getSqlName().equals(verticalColumns.get(PivotColumn.class).getSqlName())))
				                                             .get(false)
				                                             .stream()
				                                             .map(Entry::getValue)
				                                             .collect(Collectors.toCollection(LinkedList::new));
				columns.add(verticalColumns.get(PivotColumn.class));
				columns.add(verticalColumns.get(ValuesColumn.class));
				queryColumns = columns.toArray(ColumnInterface[]::new);
			} else {
				queryColumns = fieldsColumns.values().toArray(ColumnInterface[]::new);
			}
			
			query = table.select(queryColumns);
		}
		return query.where(keysPairs).setProvider(getProvider()).promise()
		            .compose(qr -> qr.toStreamOf(getClass()))
		            .joinThrow()
		            .flatMap(Stream::findFirst);
	}
	
	/**
	 * Load persistence object or create a new record if none found.
	 */
	private void instantiate(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		load(table, keysPairs, fieldsColumns)
				.ifPresentOrElse(
						this::copyFrom,
						() -> {
							log().trace(String.format("  ! No record found for persistence to load. Creating new %s.class persisted object.", getClass().getSimpleName()));
							insertAndConsume(table, fieldsColumns, fieldsValues);
						}
				);
	}
	
	/**
	 * In case of {@link Vertical} - rewrites the object, with consequences for all {@link Incremental} fields. Otherwise, check if object is persisted - update its values, or instantiate a new record.
	 */
	private void save(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		if (getClass().isAnnotationPresent(Vertical.class)) {
			delete(table, keysPairs);
			insertAndConsume(table, fieldsColumns, fieldsValues);
		} else {
			load(table, keysPairs, fieldsColumns)
					.ifPresentOrElse(
							_ -> table.update(fieldsValues.entrySet().stream()
							                              .filter(e -> !e.getKey().isAnnotationPresent(Key.class))
							                              .map(e -> ColumnSetValueComparison.of(fieldsColumns.get(e.getKey()), e.getValue()))
							                              .toArray(ColumnSetValueComparison[]::new))
							          .where(keysPairs)
							          .setProvider(getProvider())
							          .promise()
							          .thenRun(() -> log().trace("    Record updated."))
							          .joinThrow(),
							() -> insertAndConsume(table, fieldsColumns, fieldsValues)
					);
		}
	}
	
	/**
	 * Copy current object as new persisted record. Only if class defines at least one <b>@Incremental @Key</b> field.
	 *
	 * @see Incremental @Incremental
	 * @see Key @Key
	 */
	private void copyAsNew(TableInterface table, Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		if (keys.stream().noneMatch(f -> f.isAnnotationPresent(Incremental.class)))
			throw logFatalAndThrow(String.format("%s.class does not have @Incremental keys, thus copying amd saving would result in ambiguity.", getClass().getSimpleName()));
		
		if (keysAreMissingValues(true, keys, fieldsValues))
			throw logFatalAndThrow(String.format("Obligatory keys have no values. Aborting creation of %s.class.", getClass().getSimpleName()));
		
		insertAndConsume(table, fieldsColumns, fieldsValues);
	}
	
	/**
	 * Delete persistence record from database.
	 */
	private void delete(TableInterface table, ColumnsComparisonInterface[] keysPairs) {
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
	private void insertAndConsume(TableInterface table, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		List<Map<ColumnInterface, Object>> values = columnsToValues(fieldsColumns, fieldsValues);
		
		val insert = table.insert()
		                  .into(values.getFirst().keySet().toArray(ColumnInterface[]::new));
		
		values.forEach(v -> insert.values(v.values().toArray()));
		
		insert.setProvider(getProvider())
		      .promise()
		      .compose(qr -> qr.toStreamOf(getClass()))
		      .joinThrow()
		      .flatMap(Stream::findFirst)
		      .ifPresent(this::copyFrom);
	}
	
	private List<Map<ColumnInterface, Object>> columnsToValues(Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		val clazz = getClass();
		if (clazz.isAnnotationPresent(Vertical.class)) {
			val verticalColumns = getVerticalMandatoryAnnotations(clazz);
			
			Map<Boolean, List<Entry<Field, ColumnInterface>>> splitFields = fieldsColumns.entrySet().stream()
			                                                                             .filter(e -> PersistenceInterface.getPersistenceSetupAnnotationsExcluding(Key.class).stream().noneMatch(e.getKey()::isAnnotationPresent))
			                                                                             .collect(Collectors.partitioningBy(e -> !e.getKey().isAnnotationPresent(Key.class)));
			
			Map<ColumnInterface, Object> groupFields = splitFields.get(false).stream().collect(Collectors.toMap(Entry::getValue, e -> fieldsValues.get(e.getKey()), (_, b) -> b, LinkedHashMap::new));
			
			Map<Field, ColumnInterface> pivotFields = splitFields.get(true).stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (_, b) -> b, LinkedHashMap::new));
			
			return pivotFields.entrySet().stream()
			                  .map(p -> {
				                  Map<ColumnInterface, Object> group = new LinkedHashMap<>(groupFields);
				                  group.put(verticalColumns.get(PivotColumn.class), p.getValue().getSqlName());
				                  group.put(verticalColumns.get(ValuesColumn.class), String.valueOf(fieldsValues.get(p.getKey())));
				                  return group;
			                  })
			                  .toList();
		} else {
			return List.of(fieldsColumns.entrySet()
			                            .stream()
			                            .filter(e -> PersistenceInterface.getPersistenceSetupAnnotationsExcluding(Key.class).stream().noneMatch(e.getKey()::isAnnotationPresent))
			                            .collect(Collectors.toMap(
					                            Entry::getValue,
					                            e -> fieldsValues.get(e.getKey()),
					                            (a, _) -> a,
					                            LinkedHashMap::new
			                            )));
		}
		
	}
	
	/**
	 * Copy fields values based on their names from another object. Passes through visibility restrictions. Any non-matching will be skipped.
	 */
	private <T> void copyFrom(T another) {
		log().trace(String.format("  > Copy fields %s.class to %s.class...", another.getClass().getSimpleName(), getClass().getSimpleName()));
		
		Map<String, Field> anotherFields =
				Arrays.stream(another.getClass().getDeclaredFields())
				      .filter(AccessibleObject::trySetAccessible)
				      .collect(Collectors.toMap(
						      a -> a.getName().toLowerCase(),
						      a -> a
				      ));
		
		Stream.of(getClass().getDeclaredFields())
		      .filter(f -> !Tools.isSkipped(f, SkipTypes.persistence) && f.trySetAccessible())
		      .forEach(f -> {
			      try {
				      f.set(this, Optional.ofNullable(anotherFields.get(f.getName().toLowerCase())).orElseThrow(NoSuchFieldException::new).get(another));
			      } catch (NoSuchFieldException e) {
				      log().trace(String.format("    Field %s not found in %s.class. Skipped.", f.getName(), another.getClass().getSimpleName()));
			      } catch (IllegalAccessException ignored) {
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
	 * Serialize to JSON, using {@link JSON io.krystal.JSON}
	 */
	default JSONObject toJSON() {
		return JSON.from(this);
	}
	
	/**
	 * All Persistence-setup related annotations, values of which can be derived from fields or methods. Used mainly in fields-filtering during reflection.
	 */
	static List<Class<? extends Annotation>> getPersistenceSetupAnnotations() {
		return new ArrayList<>(List.of(
				Key.class,
				Incremental.class,
				Skip.class,
				ColumnsMapping.class,
				Loader.class,
				Filter.class,
				Provider.class,
				PivotColumn.class,
				ValuesColumn.class,
				UnpivotToColumns.class
		));
	}
	
	/**
	 * @see #getPersistenceSetupAnnotations()
	 */
	@SafeVarargs
	static List<Class<? extends Annotation>> getPersistenceSetupAnnotationsExcluding(Class<? extends Annotation>... annotations) {
		val list = getPersistenceSetupAnnotations();
		list.removeAll(Arrays.stream(annotations).toList());
		return list;
	}
	
	/**
	 * If the values are missing, throws {@link RuntimeException}.
	 */
	private static Map<Class<? extends Annotation>, ColumnInterface> getVerticalMandatoryAnnotations(Class<?> clazz) {
		return Stream.of(PivotColumn.class, ValuesColumn.class)
		             .collect(Collectors.toMap(
				             a -> a,
				             a -> Optional.ofNullable(Tools.getFirstAnnotatedValue(a, ColumnInterface.class, clazz, null))
				                          .orElseThrow(() -> LoggingInterface.logFatalAndThrow(LoggingInterface.logger(), "Vertical Persistence: %s is missing PivotColumn or ValuesColumn annotation.".formatted(clazz.getSimpleName())))
		             ));
	}
	
}