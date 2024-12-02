package krystal.framework.database.persistence;

import krystal.JSON;
import krystal.Skip;
import krystal.Skip.SkipTypes;
import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.*;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.persistence.annotations.Vertical.PivotColumn;
import krystal.framework.database.persistence.annotations.Vertical.UnpivotToColumns;
import krystal.framework.database.persistence.annotations.Vertical.ValuesColumn;
import krystal.framework.database.persistence.filters.StatementModifiers;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO JDoc :)
 *
 * @see Persistence#promiseAll(Class, QueryExecutorInterface, StatementModifiers, Object)
 * @see TableInterface
 * @see Vertical
 * @see ColumnsMapping
 * @see Loader
 * @see Filter
 * @see Reader
 * @see Writer
 * @see ReadOnly
 */
@FunctionalInterface
public interface PersistenceInterface extends LoggingInterface {
	
	/**
	 * Convert rows of {@link QueryResultInterface} data into stream of objects, using their {@link Reader @Reader} annotated constructors. The types of argument of particular constructor must match the data type of each column in the
	 * {@link QueryResultInterface} row. There can be a number of {@link Reader @Readers}, i.e. each responsible for handling different {@link ProviderInterface}. The class can be declared as {@link Vertical @Vertical}, in case which, the
	 * {@link QueryResultInterface} will be un-pivoted first.
	 *
	 * @see QueryResultInterface#unpivot(ColumnInterface, ColumnInterface, ColumnInterface...)
	 */
	@SuppressWarnings("unchecked")
	static <T> VirtualPromise<Stream<T>> mapQueryResult(QueryResultInterface qr, Class<T> clazz) {
		
		if (qr.rows().isEmpty()) return VirtualPromise.supply(Stream::empty);
		
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
	
	/**
	 * If the values are missing, throws {@link RuntimeException}.
	 */
	static Map<Class<? extends Annotation>, ColumnInterface> getVerticalMandatoryAnnotations(Class<?> clazz) {
		return Stream.of(PivotColumn.class, ValuesColumn.class)
		             .collect(Collectors.toMap(
				             a -> a,
				             a -> Optional.ofNullable(Tools.getFirstAnnotatedValue(a, ColumnInterface.class, clazz, null))
				                          .orElseThrow(() -> new RuntimeException("Vertical Persistence: %s are missing PivotColumn or ValuesColumn annotation.".formatted(clazz.getSimpleName())))
		             ));
	}
	
	// TODO Foreigner annotation: Marks field for which the automatic Reader, Writer and Remover action will be performed based on provided link class and column interfaces
	
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
	 * Returns {@link ProviderInterface} used to loadFromDatabase instance of an object of provided class.
	 *
	 * @see Provider @Provider
	 */
	static <T extends PersistenceInterface, D extends T> @Nullable ProviderInterface getProvider(Class<T> clazz, @Nullable D optionalDummyType) {
		return getFirstAnnotatedValueOrInvokeDefaultWithOptionalDummy(clazz, Provider.class, ProviderInterface.class, optionalDummyType, PersistenceInterface::getProvider);
	}
	
	/**
	 * Declared with {@link Provider @Provider}, or {@link krystal.framework.KrystalFramework#getDefaultProvider() KrystalFramework#getDefaultProvider()} as default.
	 */
	default ProviderInterface getProvider() {
		return Optional.ofNullable(Tools.getFirstAnnotatedValue(Provider.class, ProviderInterface.class, this)).orElse(KrystalFramework.getDefaultProvider());
	}
	
	/**
	 * Returns {@link SelectStatement} used to loadFromDatabase instance of an object of provided class, or default, or throws error if for some reason it's missing.
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
			
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()), e);
		}
	}
	
	/**
	 * Returns first custom {@link Loader @Loader} query used to loadFromDatabase this class or {@code null} if missing.
	 *
	 * @see #getSelectQuery(Class, Object)
	 */
	default @Nullable SelectStatement getSelectQuery() {
		return getSelectQuery(getClass(), this);
	}
	
	@SuppressWarnings("unchecked")
	static <T> Function<SelectStatement, WhereClause> getFilteredQuery(Class<? extends T> clazz, @Nullable T optionalDummyType) {
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
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()), e);
		}
	}
	
	default Function<SelectStatement, WhereClause> getFilteredQuery() {
		return getFilteredQuery(getClass(), this);
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
			throw new RuntimeException("Class %s requires no args constructor, to perform annotated parameters loading.".formatted(clazz.getSimpleName()), e);
		}
	}
	
	/**
	 * Mappings of fields names and corresponding {@link ColumnInterface columns} in database, if other than plain names.
	 *
	 * @param invokedOn
	 * 		Used with {@link ColumnsMapping} annotated methods or fields (instead of {@link Enum}). If {@code null}, the method or field must be {@code static} to return value.
	 * @see #getFieldsColumns(Class, Object)
	 * @see ColumnsMap
	 * @see ColumnsMapping @ColumnsMapping
	 */
	@SuppressWarnings("unchecked")
	static <E extends Enum<?> & ColumnInterface> ColumnsMap getFieldsToColumnsMap(Class<?> clazz, @Nullable Object invokedOn) {
		return Arrays.stream(clazz.getDeclaredClasses())
		             .filter(c -> c.isAnnotationPresent(ColumnsMapping.class) && ColumnInterface.class.isAssignableFrom(c) && Enum.class.isAssignableFrom(c))
		             .findFirst()
		             .map(c -> ColumnsMap.fromColumnInterfaceEnum(clazz, (Class<E>) c))
		             .or(() -> Optional.ofNullable(Tools.getFirstAnnotatedValue(ColumnsMapping.class, ColumnsMap.class, clazz, invokedOn)))
		             .orElse(ColumnsMap.empty());
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
	static Map<Field, ColumnInterface> getFieldsColumns(Class<?> clazz, @Nullable Object invokedOn) {
		val map = getFieldsToColumnsMap(clazz, invokedOn);
		// collect for all fields, either mapping or name
		return Stream.of(clazz.getDeclaredFields())
		             .filter(f -> !Tools.isSkipped(f, SkipTypes.persistence))
		             .collect(Collectors.toMap(
				             f -> f,
				             f -> Optional.ofNullable(map.columns().get(f)).orElse(f::getName),
				             (a, b) -> a,
				             LinkedHashMap::new
		             ));
	}
	
	default Map<Field, Object> getWriters() {
		val map = new HashMap<Field, Object>();
		
		// collector does not allow null values -> foreach loop
		Stream.of(getClass().getDeclaredMethods())
		      .filter(m -> m.trySetAccessible() && (
				      m.getName().toLowerCase().matches("^write.*?")
						      || m.isAnnotationPresent(Writer.class)))
		      .forEach(m -> {
			      if (m.getReturnType().equals(void.class)) return;
			      val fld = Arrays.stream(getClass().getDeclaredFields())
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
					      });
			      Object value = null;
			      try {
				      value = m.invoke(this);
			      } catch (IllegalAccessException _) {
			      } catch (InvocationTargetException e) {
				      val cause = e.getCause();
				      if (!(cause instanceof NullPointerException))
					      throw new RuntimeException("PersistenceInterface: %s writer method throws exception: %s.".formatted(m.getName(), cause.getMessage()), e);
			      }
			      map.put(fld, value);
		      });
		
		return map;
	}
	
	default void runReaders() {
		Tools.runAnnotatedMethods(Reader.class, this);
	}
	
	default void runWriters() {
		Tools.runAnnotatedMethods(Writer.class, this);
	}
	
	default void runRemovers() {
		Tools.runAnnotatedMethods(Remover.class, this);
	}
	
	/**
	 * Fields values computed with writers or if none are set or writer returns {@code null} - plain values.
	 *
	 * @see Writer @Writer
	 */
	default Map<Field, Object> getFieldsValues() {
		val m = getWriters();
		val map = new HashMap<Field, Object>();
		
		// collector does not allow null values -> foreach loop
		Stream.of(getClass().getDeclaredFields())
		      .filter(f -> !Tools.isSkipped(f, SkipTypes.persistence))
		      .forEach(f -> map.put(f, Optional.ofNullable(m.get(f)).orElseGet(
				      () -> {
					      try {
						      f.setAccessible(true);
						      return f.get(this);
					      } catch (IllegalAccessException e) {
						      throw new RuntimeException(e);
					      }
				      }
		      )));
		
		return map;
	}
	
	private ColumnsComparisonInterface[] getKeyValuePairs(Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues, boolean includeIfNull) {
		boolean finalIncludeIfNull = Optional.ofNullable(getClass().getAnnotation(SeparateKeys.class)).map(SeparateKeys::includeIfNull).orElse(includeIfNull);
		return keys.stream()
		           .map(field -> new ColumnToValueComparison(fieldsColumns.get(field), ComparisonOperator.IN, fieldsValues.get(field)))
		           .filter(cvc -> !cvc.values().isEmpty() || finalIncludeIfNull)
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
			throw new RuntimeException(String.format("%s.class is missing @Keys - can not perform single persistence operations.", getClass().getSimpleName()));
		
		if (execution != PersistenceExecutions.load && classIsReadOnly(getClass()))
			throw new RuntimeException(String.format("%s.class is marked as @ReadOnly.", getClass().getSimpleName()));
		
		val keys = getKeys();
		val fieldsValues = getFieldsValues();
		
		if (keysAreMissingValues(false, keys, fieldsValues)) {
			if (!((execution == PersistenceExecutions.instantiate || execution == PersistenceExecutions.save) && !keysAreMissingValues(true, keys, fieldsValues)))
				throw new RuntimeException(String.format("Keys for %s.class have no values. Aborting %s.", getClass().getSimpleName(), execution));
		}
		
		val fieldsColumns = getFieldsColumns(getClass(), this);
		
		ColumnsComparisonInterface[] keysPairs = getKeyValuePairs(keys, fieldsColumns, fieldsValues, execution != PersistenceExecutions.instantiate);
		
		val table = getTable();
		
		switch (execution) {
			case load -> PersistenceMemory.getInstance()
			                              .filter(mem -> !getClass().isAnnotationPresent(Fresh.class))
			                              .map(mem -> mem.get(hashKeys(getClass(), fieldsValues)))
			                              .ifPresentOrElse(this::copyFrom,
			                                               () -> loadFromDatabase(table, keysPairs, fieldsColumns)
					                                                     .ifPresentOrElse(another -> {
						                                                     copyFrom(another);
						                                                     runReaders();
					                                                     }, () -> log().trace(String.format("  ! No record found for persistence to loadFromDatabase %s.class.", getClass().getSimpleName()))));
			case instantiate -> instantiateInDatabase(table, keysPairs, fieldsColumns, fieldsValues);
			case delete -> deleteFromDatabase(table, keysPairs);
			case save -> saveToDatabase(table, keysPairs, fieldsColumns, fieldsValues);
			case copyAsNew -> copyAsNew(table, keys, fieldsColumns, fieldsValues);
		}
		
	}
	
	/**
	 * Load persistence object from database.
	 */
	private Optional<? extends PersistenceInterface> loadFromDatabase(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns) {
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
	private void instantiateInDatabase(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		loadFromDatabase(table, keysPairs, fieldsColumns)
				.ifPresentOrElse(
						another -> {
							copyFrom(another);
							runReaders();
						},
						() -> {
							log().trace(String.format("  ! No record found for persistence to loadFromDatabase. Creating new %s.class persisted object.", getClass().getSimpleName()));
							insertInDatabaseAndConsume(table, fieldsColumns, fieldsValues);
						}
				);
	}
	
	/**
	 * In case of {@link Vertical} - rewrites the object, with consequences for all {@link Incremental} fields. Otherwise, check if object is persisted - update its values, or instantiate a new record.
	 */
	private void saveToDatabase(TableInterface table, ColumnsComparisonInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		if (getClass().isAnnotationPresent(Vertical.class)) {
			deleteFromDatabase(table, keysPairs);
			insertInDatabaseAndConsume(table, fieldsColumns, fieldsValues);
		} else {
			loadFromDatabase(table, keysPairs, fieldsColumns)
					.ifPresentOrElse(
							_ -> table.update(fieldsValues.entrySet().stream()
							                              .filter(e -> !e.getKey().isAnnotationPresent(Key.class))
							                              .map(e -> ColumnSetValueComparison.of(fieldsColumns.get(e.getKey()), e.getValue()))
							                              .toArray(ColumnSetValueComparison[]::new))
							          .where(keysPairs)
							          .setProvider(getProvider())
							          .promise()
							          .thenRun(this::runWriters)
							          .thenRun(() -> log().trace("    Record updated."))
							          .joinThrow(),
							() -> insertInDatabaseAndConsume(table, fieldsColumns, fieldsValues)
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
			throw new RuntimeException(String.format("%s.class does not have @Incremental keys, thus copying amd saving would result in ambiguity.", getClass().getSimpleName()));
		
		if (keysAreMissingValues(true, keys, fieldsValues))
			throw new RuntimeException(String.format("Obligatory keys have no values. Aborting creation of %s.class.", getClass().getSimpleName()));
		
		insertInDatabaseAndConsume(table, fieldsColumns, fieldsValues);
	}
	
	/**
	 * Delete persistence record from database.
	 */
	private void deleteFromDatabase(TableInterface table, ColumnsComparisonInterface[] keysPairs) {
		VirtualPromise.run(this::runRemovers)
		              .compose(() -> table.delete().where(keysPairs).setProvider(getProvider()).promise())
		              .map(qr -> {
			              if (getProvider().getDriver().getSupportedOutputtingStatements().contains(QueryType.DELETE)) {
				              return qr.renderAsStringTable();
			              } else {
				              return qr.getResult().map(String::valueOf);
			              }
		              })
		              .accept(s -> log().trace("  ! Persisted object deleted from database. Deleted rows: {}", s))
		              .thenRun(() -> PersistenceMemory.getInstance().ifPresent(memory -> memory.remove(this.hashKeys())))
		              .join();
	}
	
	/*
	 * Technical stuff
	 */
	
	/**
	 * Create persistence record and consume it.
	 */
	private void insertInDatabaseAndConsume(TableInterface table, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
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
		runWriters();
		if (!getClass().isAnnotationPresent(Fresh.class))
			PersistenceMemory.getInstance()
			                 .ifPresent(memory -> memory.put(hashKeys(getClass(), fieldsValues), this, memory.getIntervalsCount()));
	}
	
	/**
	 * @apiNote Is a {@link List} because handles {@link Vertical}.
	 */
	private List<Map<ColumnInterface, Object>> columnsToValues(Map<Field, ColumnInterface> fieldsColumns, Map<Field, Object> fieldsValues) {
		val clazz = getClass();
		if (clazz.isAnnotationPresent(Vertical.class)) {
			val verticalColumns = getVerticalMandatoryAnnotations(clazz);
			
			Map<Boolean, List<Entry<Field, ColumnInterface>>> splitFields = fieldsColumns.entrySet().stream()
			                                                                             .filter(e -> PersistenceInterface.getPersistenceSetupAnnotationsExcluding(Key.class).stream().noneMatch(e.getKey()::isAnnotationPresent))
			                                                                             .collect(Collectors.partitioningBy(e -> !e.getKey().isAnnotationPresent(Key.class)));
			
			// null not allowed in Collectors.toMap merge function
			val groupFields = new LinkedHashMap<ColumnInterface, Object>(splitFields.get(false).size());
			splitFields.get(false).forEach(e -> groupFields.put(e.getValue(), fieldsValues.get(e.getKey())));
			
			val pivotFields = new LinkedHashMap<Field, ColumnInterface>(splitFields.get(true).size());
			splitFields.get(true).forEach(e -> pivotFields.put(e.getKey(), e.getValue()));
			
			// many rows of data
			return pivotFields.entrySet().stream()
			                  .map(p -> {
				                  Map<ColumnInterface, Object> group = new LinkedHashMap<>(groupFields);
				                  group.put(verticalColumns.get(PivotColumn.class), p.getValue().getSqlName());
				                  group.put(verticalColumns.get(ValuesColumn.class), String.valueOf(fieldsValues.get(p.getKey())));
				                  return group;
			                  })
			                  .toList();
		} else {
			// single row of data
			val result = new LinkedHashMap<ColumnInterface, Object>();
			fieldsColumns.entrySet()
			             .stream()
			             .filter(e -> PersistenceInterface.getPersistenceSetupAnnotationsExcluding(Key.class).stream().noneMatch(e.getKey()::isAnnotationPresent))
			             .forEach(e -> result.put(e.getValue(), fieldsValues.get(e.getKey())));
			return List.of(result);
		}
		
	}
	
	/**
	 * Copy fields values based on their names from another object. Passes through visibility restrictions. Any non-matching will be skipped.
	 */
	private <T> void copyFrom(T another) {
		log().trace(String.format("  > Copy fields of %s.class to %s.class...", another.getClass().getSimpleName(), getClass().getSimpleName()));
		
		val anotherFields = another.getClass().getDeclaredFields();
		val values = new HashMap<String, Object>(anotherFields.length);
		for (val f : anotherFields) {
			if (!f.trySetAccessible()) continue;
			try {
				values.put(f.getName(), f.get(another));
			} catch (IllegalAccessException _) {
			}
		}
		
		Stream.of(getClass().getDeclaredFields())
		      .filter(AccessibleObject::trySetAccessible)
		      .forEach(f -> Optional.ofNullable(values.get(f.getName())).ifPresent(value -> {
			      try {
				      f.set(this, value);
			      } catch (IllegalAccessException _) {
			      }
		      }));
		log().trace("    Copy done.");
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
		return JSON.fromObject(this);
	}
	
	/**
	 * Create individual {@link String} representation for provided class and fields values. Limit to fields which have {@link Key} annotation present or take all fields if not.
	 */
	static String hashKeys(Class<?> clazz, Map<Field, Object> fieldsValues) {
		val hasKeys = PersistenceInterface.classHasKeys(clazz);
		
		val str = new StringBuilder(clazz.getName());
		if (clazz.isAnnotationPresent(Memorized.class)) str.append("@Memorized");
		str.append('>');
		
		fieldsValues.entrySet()
		            .stream()
		            .filter(e -> !hasKeys || e.getKey().isAnnotationPresent(Key.class))
		            .forEach(e -> str.append(e.getKey().getName()).append('=').append(e.getValue()).append('|'));
		
		return str.toString();
	}
	
	/**
	 * Create individual {@link String} representation of this object's {@link Key Keys} or all fields if none present.
	 */
	default String hashKeys() {
		return hashKeys(getClass(), getFieldsValues());
	}
	
	default void memorize(PersistenceMemory persistenceMemory) {
		persistenceMemory.put(this);
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
	
}