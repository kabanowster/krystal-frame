package framework.database.persistence;

import framework.database.abstraction.*;
import framework.database.persistence.annotations.*;
import framework.database.queryfactory.ColumnIsPair;
import framework.database.queryfactory.ColumnOperators;
import framework.database.queryfactory.ColumnSetPair;
import framework.database.queryfactory.ColumnsPairingInterface;
import framework.logging.LoggingInterface;
import krystal.CompletablePresent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO JDoc :)
 */
public interface PersistenceInterface extends LoggingInterface {
	
	/*
	 * Static tools
	 */
	
	/**
	 * Get all persisted objects from the database of particular type. The class must support empty (no arguments) constructor. {@link QueryExecutorInterface} here is present for initial Spring injections support. You can use {@link #getAll(Class)} in
	 * cases following after.
	 */
	static <T extends PersistenceInterface> Stream<T> getAll(Class<T> clazz, QueryExecutorInterface queryExecutor) {
		try {
			Constructor<T> emptyConstructor = clazz.getDeclaredConstructor();
			TableInterface table = emptyConstructor.newInstance().getTable();
			return table.select().pack().execute(queryExecutor).toStreamOf(clazz);
		} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * To be utilised after initial Spring injections (after {@link QueryExecutorInterface} is initialised).
	 *
	 * @see #getAll(Class, QueryExecutorInterface)
	 */
	static <T extends PersistenceInterface> Stream<T> getAll(Class<T> clazz) {
		return getAll(clazz, QueryExecutorInterface.getInstance());
	}
	
	/**
	 * Check, if the class specified is valid for persistence executions.
	 */
	static boolean persistenceIsNotValid(Class<?> clazz) {
		return clazz.isAnnotationPresent(DirectReadOnly.class)
				|| Stream.of(clazz.getDeclaredFields()).noneMatch(f -> f.isAnnotationPresent(Key.class));
	}
	
	static boolean classIsReadOnly(Class<?> clazz) {
		return clazz.isAnnotationPresent(ReadOnly.class);
	}
	
	/**
	 * Convert rows of {@link QueryResultInterface} data into stream of objects, using their {@link Reader @Reader} annotated constructors. The types of argument of particular constructor must match the data type of each column in the
	 * {@link QueryResultInterface} row. There can be a number of {@link Reader @Readers}, i.e. each responsible for handling different {@link ProviderInterface}.
	 */
	@SuppressWarnings("unchecked")
	static <T> Stream<T> mapQueryResult(QueryResultInterface qr, Class<T> clazz) {
		return qr.rows().stream().map(row -> {
			try {
				var constructor =
						(Constructor<T>) Stream.of(clazz.getDeclaredConstructors())
						                       .filter(c -> c.isAnnotationPresent(Reader.class)
								                       && Arrays.equals(c.getParameterTypes(), qr.columns().values().toArray(Class<?>[]::new)))
						                       .findFirst()
						                       .orElseThrow();
				constructor.setAccessible(true);
				return constructor.newInstance(row.values().toArray());
			} catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchElementException e) {
				throw new RuntimeException(e);
			}
		});
	}
	
	/*
	 * Required setup
	 */
	
	/**
	 * Table linked with object's persistence.
	 *
	 * @see TableInterface
	 */
	TableInterface getTable();
	
	/**
	 * Provider of the table, namely the database.
	 *
	 * @see ProviderInterface
	 */
	ProviderInterface getProvider();
	
	/**
	 * Mappings of fields names and writing methods to database, if other than plain values. Return null/empty map if n/a. If you plan to work with null fields - keep this as a method computing value on demand.
	 */
	Writers getWriters();
	
	/**
	 * Mappings of fields names and corresponding columns in database, if other than plain names. Return null/empty map if n/a.
	 */
	ColumnsMap getFieldsToColumnsMap();
	
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
	 * Mapping of fields to database {@link ColumnInterface columns}, if different from fields names.
	 */
	private Map<Field, ColumnInterface> getFieldsColumns() {
		return CompletablePresent
				// read overwritten setup
				.supply(() -> Optional.ofNullable(getFieldsToColumnsMap())
				                      .orElse(ColumnsMap.empty())
				                      .columns().entrySet().stream()
				                      .collect(Collectors.toMap(
						                      e -> {
							                      try {
								                      return getClass().getDeclaredField(e.getKey());
							                      } catch (NoSuchFieldException ex) {
								                      throw new RuntimeException(ex);
							                      }
						                      },
						                      Map.Entry::getValue
				                      )))
				// collect for all fields, either mapping or name
				.thenApply(m -> Stream.of(getClass().getDeclaredFields())
				                      .filter(f -> !f.isAnnotationPresent(Skip.class))
				                      .collect(Collectors.toMap(
						                      f -> f,
						                      f -> Optional.ofNullable(m.get(f)).orElse(() -> String.format("[%s]", f.getName()))
				                      )))
				.getResult().get();
	}
	
	/**
	 * Fields values computed with {@link #getWriters() writers} or plain, if no mappings are set.
	 */
	private Map<Field, Optional<Object>> getFieldsValues() {
		return CompletablePresent
				// writers output
				.supply(() -> Optional.ofNullable(getWriters())
				                      .orElse(Writers.empty())
				                      .writers().entrySet().stream()
				                      .collect(Collectors.toMap(
						                      e -> {
							                      try {
								                      return getClass().getDeclaredField(e.getKey());
							                      } catch (NoSuchFieldException ex) {
								                      throw new RuntimeException(ex);
							                      }
						                      },
						                      e -> {
							                      try {
								                      return Optional.ofNullable(e.getValue().get());
							                      } catch (NullPointerException ex) {
								                      return Optional.empty();
							                      }
						                      }
				                      )))
				// collect for all fields, either mapping or field's value
				.thenApply(m -> Stream.of(getClass().getDeclaredFields())
				                      .filter(f -> !f.isAnnotationPresent(Skip.class))
				                      .collect(Collectors.toMap(
						                      f -> f,
						                      f -> Optional.ofNullable(m.get(f)).orElseGet(
								                      () -> {
									                      try {
										                      f.setAccessible(true);
										                      return Optional.ofNullable(f.get(this));
									                      } catch (IllegalAccessException e) {
										                      throw new RuntimeException(e);
									                      }
								                      }
						                      ),
						                      (f1, f2) -> f1,
						                      LinkedHashMap::new
				                      )))
				.getResult().get();
	}
	
	private ColumnsPairingInterface[] getKeyPairs(Set<Field> keys, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Optional<Object>> fieldsValues) {
		return keys.stream()
		           .map(field -> new ColumnIsPair(fieldsColumns.get(field), ColumnOperators.In, fieldsValues.get(field).stream().toList()))
		           .toArray(ColumnsPairingInterface[]::new);
	}
	
	private boolean keysHaveNoValues(boolean nonIncrementalOnly, Set<Field> keys, Map<Field, Optional<Object>> fieldsValues) {
		for (Field f : keys.stream().filter(f -> !nonIncrementalOnly || !f.isAnnotationPresent(Incremental.class)).toList())
			if (fieldsValues.get(f).isEmpty()) return true;
		return false;
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
		execute(PersistenceExecutions.copyAsNew);
	}
	
	/*
	 * Persistence executions
	 */
	
	/**
	 * Factory method for persistence executions. Performs initial checks and data collection.
	 */
	private void execute(PersistenceExecutions execution) {
		log().trace(">>> Performing persistence execution: " + execution.toString());
		
		if (persistenceIsNotValid(getClass()))
			throw new RuntimeException(String.format("  ! %s.class is not properly set for persistence.", getClass().getSimpleName()));
		
		if (execution != PersistenceExecutions.load && classIsReadOnly(getClass()))
			throw new RuntimeException(String.format("  ! %s.class is marked as @ReadOnly.", getClass().getSimpleName()));
		
		var keys = getKeys();
		var fieldsValues = getFieldsValues();
		
		if (keysHaveNoValues(false, keys, fieldsValues))
			throw new RuntimeException(String.format("  ! Keys for %s.class are not set. Aborting %s.", getClass().getSimpleName(), execution));
		
		var fieldsColumns = getFieldsColumns();
		
		ColumnsPairingInterface[] keysPairs = getKeyPairs(keys, fieldsColumns, fieldsValues);
		
		var table = getTable();
		
		switch (execution) {
			case load -> load(table, keysPairs).ifPresentOrElse(this::copyFrom, () -> log().trace(String.format("  ! No record found for persistence to load %s.class.", getClass().getSimpleName())));
			case instantiate -> instantiate(table, keysPairs, fieldsValues);
			case delete -> delete(table, keysPairs);
			case save -> save(table, keysPairs, fieldsColumns, fieldsValues);
			case copyAsNew -> copyAsNew(table, keys, fieldsValues);
		}
		
	}
	
	/**
	 * Load persistence object from database in form of Optional.
	 */
	private Optional<? extends PersistenceInterface> load(TableInterface table, ColumnsPairingInterface[] keysPairs) {
		return table.select().where(keysPairs).providedBy(getProvider()).execute().toStreamOf(getClass()).findFirst();
	}
	
	/**
	 * Load persistence object or create a new record if none found.
	 */
	private void instantiate(TableInterface table, ColumnsPairingInterface[] keysPairs, Map<Field, Optional<Object>> fieldsValues) {
		load(table, keysPairs).ifPresentOrElse(
				this::copyFrom,
				() -> {
					log().trace(String.format("  ! No record found for persistence to load. Creating new %s.class persisted object.", getClass().getSimpleName()));
					insertAndConsumeSelf(table, fieldsValues);
				}
		);
	}
	
	/**
	 * Check if object is persisted and update its values, or create a new record.
	 */
	private void save(TableInterface table, ColumnsPairingInterface[] keysPairs, Map<Field, ColumnInterface> fieldsColumns, Map<Field, Optional<Object>> fieldsValues) {
		
		if (load(table, keysPairs).isEmpty()) {
			insertAndConsumeSelf(table, fieldsValues);
			return;
		}
		
		table.update(fieldsValues.entrySet().stream()
		                         .filter(e -> !e.getKey().isAnnotationPresent(Key.class))
		                         .map(e -> {
			                         if (e.getValue().isPresent())
				                         return new ColumnSetPair(fieldsColumns.get(e.getKey()), e.getValue().get());
			                         else return null;
		                         })
		                         .filter(Objects::nonNull)
		                         .toArray(ColumnSetPair[]::new))
		     .where(keysPairs)
		     .providedBy(getProvider())
		     .execute();
	}
	
	/**
	 * Copy current object as new persisted record. Only if class defines at least one <b>@Incremental @Key</b> field.
	 *
	 * @see Incremental
	 * @see Key
	 */
	private void copyAsNew(TableInterface table, Set<Field> keys, Map<Field, Optional<Object>> fieldsValues) {
		
		if (keys.stream().noneMatch(f -> f.isAnnotationPresent(Incremental.class)))
			throw new RuntimeException(String.format("  ! %s.class does not have @Incremental keys, thus copying amd saving would result in ambiguity.", getClass().getSimpleName()));
		
		if (keysHaveNoValues(true, keys, fieldsValues))
			throw new RuntimeException(String.format("  ! Obligatory keys have no values. Aborting creation of %s.class.", getClass().getSimpleName()));
		
		insertAndConsumeSelf(table, fieldsValues);
	}
	
	/**
	 * Delete persistence record from database.
	 */
	private void delete(TableInterface table, ColumnsPairingInterface[] keysPairs) {
		int deleted = (int) table.delete().where(keysPairs).providedBy(getProvider()).execute().getResult().orElse(0);
		if (deleted > 0)
			log().trace("  ! Persisted object deleted from database. Deleted rows: " + deleted);
		else
			log().trace("  ! Persisted object not deleted from database.");
	}
	
	/*
	 * Technical stuff
	 */
	
	/**
	 * Create persistence record and consume it.
	 */
	private void insertAndConsumeSelf(TableInterface table, Map<Field, Optional<Object>> fieldsValues) {
		table.insert()
		     .values(fieldsValues.entrySet().stream()
		                         .filter(e -> !e.getKey().isAnnotationPresent(Incremental.class))
		                         .map(e -> e.getValue().orElse(null))
		                         .toArray())
		     .providedBy(getProvider())
		     .execute()
		     .toStreamOf(getClass())
		     .findFirst()
		     .ifPresentOrElse(
				     this::copyFrom,
				     () -> {
					     throw new NoSuchElementException();
				     }
		     );
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
		log().trace("    Copy successful.\n" + this);
	}
	
}