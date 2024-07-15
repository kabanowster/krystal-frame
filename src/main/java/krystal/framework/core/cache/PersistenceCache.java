package krystal.framework.core.cache;

import krystal.Tools;
import krystal.VirtualPromise;
import krystal.framework.KrystalFramework;
import krystal.framework.core.flow.ScheduledTaskInterface;
import krystal.framework.core.flow.TasksSchedulerInterface;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.annotations.Key;
import krystal.framework.database.queryfactory.WhereClause;
import krystal.framework.logging.LoggingWrapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for creating and managing custom caches ({@link Collection Collections} and {@link Map Maps}) of {@link PersistenceInterface} objects.
 */
@SuppressWarnings("unchecked")
@Log4j2
public abstract class PersistenceCache implements TasksSchedulerInterface {
	
	private static @Getter @Setter boolean cachedResourcesOnly = false;
	
	/*
	 * Get collections
	 */
	
	public <T> Optional<Collection<T>> getCollection(Class<T> containedClass) {
		return getCollections().filter(field -> Tools.determineParameterTypes(field.getGenericType()).types()[0].equals(containedClass))
		                       .findAny()
		                       .map(f -> {
			                       try {
				                       return f.get(this);
			                       } catch (IllegalAccessException _) {
				                       // filtered-out at this point by cacheCollectionsSelector()
				                       return null;
			                       }
		                       })
		                       .map(c -> (Collection<T>) c);
	}
	
	public <T> Optional<Map<T, Object>> getMap(Class<T> keyClass) {
		return getMaps().filter(field -> Tools.determineParameterTypes(field.getGenericType()).types()[0].equals(keyClass))
		                .findAny()
		                .map(f -> {
			                try {
				                return f.get(this);
			                } catch (IllegalAccessException _) {
				                // filtered-out at this point by cacheCollectionsSelector()
				                return null;
			                }
		                })
		                .map(c -> (Map<T, Object>) c);
	}
	
	private Supplier<Stream<Field>> cacheCollectionsSelector() {
		return () -> Arrays.stream(getClass().getDeclaredFields()).filter(field -> field.trySetAccessible() && field.isAnnotationPresent(CacheCollection.class));
	}
	
	private Stream<Field> getCollections() {
		return cacheCollectionsSelector().get().filter(f -> Collection.class.isAssignableFrom(f.getType()));
	}
	
	private Stream<Field> getMaps() {
		return cacheCollectionsSelector().get().filter(f -> Map.class.isAssignableFrom(f.getType()));
	}
	
	/*
	 * Loading
	 */
	
	public <T> Optional<T> cache(Supplier<T> objectSupplier, @Nullable Predicate<T> objectValidator, @Nullable Collection<T> toCollection) {
		val object = objectSupplier.get();
		if (object != null && (objectValidator == null || objectValidator.test(object))) {
			Optional.ofNullable(toCollection)
			        .or(() -> getCollection((Class<T>) object.getClass()))
			        .map(c -> {
				        c.add(object);
				        return object;
			        });
		}
		return Optional.empty();
	}
	
	public <T> VirtualPromise<Void> loadCacheCollection(Class<T> clazz, @Nullable UnaryOperator<WhereClause> queryFilter) {
		val collection = getCollection(clazz).orElseThrow();
		return PersistenceInterface.promiseAll(clazz, queryFilter)
		                           .map(Stream::toList)
		                           .accept(collection::addAll)
		                           .catchRun(e -> log.fatal("Failed to load collection of {}.class", clazz.getSimpleName(), e));
	}
	
	public <T> VirtualPromise<Void> loadCacheCollection(Class<T> clazz) {
		return loadCacheCollection(clazz, null);
	}
	
	public void clearCaches() {
		clearCaches(getCollections().map(f -> {
			            try {
				            return f.get(this);
			            } catch (IllegalAccessException _) {
				            return null;
			            }
		            }).map(c -> (Collection<Object>) c),
		            getMaps().map(f -> {
			            try {
				            return f.get(this);
			            } catch (IllegalAccessException _) {
				            return null;
			            }
		            }).map(m -> (Map<Object, Object>) m)
		);
	}
	
	public void clearCaches(@Nullable Stream<Collection<Object>> collections, @Nullable Stream<Map<Object, Object>> maps) {
		if (collections != null) collections.forEach(Collection::clear);
		if (maps != null) maps.forEach(Map::clear);
		if (collections != null || maps != null) log.info("    Caches cleared.");
	}
	
	
	/*
	 * Get objects
	 */
	
	/**
	 * Filtering by {@link Key @Key} fields values.
	 *
	 * @see #getOrConstructor(Class, Predicate, Map, Object...)
	 */
	public <T> Optional<T> getWithKeysOrConstructor(Class<T> clazz, @Nullable Predicate<T> objectValidation, List<Object> keysValues, Object... otherConstructorParams) {
		if (keysValues.isEmpty() && otherConstructorParams.length == 0) return Optional.empty();
		return getOrConstructor(clazz, objectValidation, getKeysValuesMap(clazz, keysValues), otherConstructorParams);
	}
	
	/**
	 * Filtering by any provided field by its name.
	 *
	 * @see #getOrConstructor(Class, Predicate, Map, Object...)
	 */
	public <T> Optional<T> getWithFieldsOrConstructor(Class<T> clazz, @Nullable Predicate<T> objectValidation, Map<String, Object> fieldValuesMap, Object... otherConstructorParams) {
		if (fieldValuesMap.isEmpty() && otherConstructorParams.length == 0) return Optional.empty();
		return getOrConstructor(clazz, objectValidation, getFieldValuesMap(clazz, fieldValuesMap), otherConstructorParams);
	}
	
	/**
	 * @param objectValidator
	 * 		If provided and not found within {@link CacheCollection @CacheCollection} collection, the resulted object of invoking the constructor will be validated with predicate.
	 */
	private <T> Optional<T> getOrConstructor(Class<T> clazz, @Nullable Predicate<T> objectValidator, Map<Field, Object> fieldsValues, Object... otherConstructorParams) {
		val cacheCollection = getCollection(clazz).orElseThrow();
		return (Optional<T>) cacheCollection.stream()
		                                    .filter(o -> fieldsEquals(o, fieldsValues))
		                                    .findAny()
		                                    .or(() -> {
			                                    if (cachedResourcesOnly) return Optional.empty();
			                                    return cache(
					                                    () -> {
						                                    try {
							                                    return clazz.getDeclaredConstructor(Stream.concat(fieldsValues.keySet().stream().map(Field::getType),
							                                                                                      Arrays.stream(otherConstructorParams).map(Object::getClass))
							                                                                              .toArray(Class[]::new))
							                                                .newInstance(Stream.concat(fieldsValues.values().stream(),
							                                                                           Arrays.stream(otherConstructorParams))
							                                                                   .toArray());
						                                    } catch (NoSuchMethodException e) {
							                                    throw new RuntimeException("  ! Constructor is missing for provided keys types.", e);
						                                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
							                                    throw new RuntimeException(e);
						                                    }
					                                    },
					                                    objectValidator,
					                                    cacheCollection
			                                    );
		                                    });
	}
	
	private Map<Field, Object> getFieldValuesMap(Class<?> clazz, Map<String, Object> fieldValuesMap) {
		return fieldValuesMap.entrySet().stream()
		                     .collect(Collectors.toMap(
				                     e -> Arrays.stream(clazz.getDeclaredFields()).filter(f -> f.getName().equals(e.getKey())).findAny().orElseThrow(),
				                     Entry::getValue
		                     ));
	}
	
	private Map<Field, Object> getKeysValuesMap(Class<?> clazz, List<Object> keyValues) {
		val keyFields = Arrays.stream(clazz.getDeclaredFields())
		                      .filter(AccessibleObject::trySetAccessible)
		                      .filter(f -> f.isAnnotationPresent(Key.class))
		                      .toList();
		try {
			return keyFields.stream().collect(Collectors.toMap(
					f -> f,
					f -> keyValues.get(keyFields.indexOf(f))
			));
		} catch (IndexOutOfBoundsException e) {
			throw new RuntimeException("  ! Wrong number of provided key values or fields in class %s.".formatted(clazz.getName()), e);
		}
	}
	
	private boolean fieldsEquals(Object obj, Map<Field, Object> fieldsValues) {
		for (val entry : fieldsValues.entrySet()) {
			try {
				if (!entry.getKey().get(obj).equals(entry.getValue()))
					return false;
			} catch (IllegalAccessException _) {
				return false;
			}
		}
		return true;
	}
	
	public static Optional<PersistenceCache> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(PersistenceCache.class));
		} catch (NullPointerException e) {
			return Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingWrapper.ROOT_LOGGER.fatal("!!! There is no registered PersistenceCache within Spring context.", e);
			return Optional.empty();
		}
	}
	
	public void startSchedule(DefaultCacheTask defaultCacheTask, long interval, TimeUnit unit) {
		Runnable runnable =
				switch (defaultCacheTask) {
					case AUTO_CLEAR -> this::clearCaches;
				};
		startSchedule(defaultCacheTask, runnable, interval, unit);
	}
	
	public enum DefaultCacheTask implements ScheduledTaskInterface {
		AUTO_CLEAR;
	}
	
}