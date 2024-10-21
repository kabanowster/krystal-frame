package krystal.framework.database.persistence;

import krystal.framework.KrystalFramework;
import krystal.framework.logging.LoggingWrapper;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Each {@link PersistenceInterface} object loaded are saved in memory for quick access.
 */
public class PersistenceMemory {
	
	private static @Setter int defaultMonitorInterval = 1000;
	private static @Setter int defaultIntervalsCount = 3;
	private static @Setter int parallelismThreshold = 50;
	
	private Thread monitorThread;
	private final ConcurrentHashMap<String, PersistenceInterface> persistenceMap;
	private final ConcurrentHashMap<String, Integer> persistenceTimeout;
	private final List<String> persistenceTrash;
	private @Setter int monitorInterval;
	private @Setter int intervalsCount;
	
	public PersistenceMemory() {
		persistenceMap = new ConcurrentHashMap<>();
		persistenceTimeout = new ConcurrentHashMap<>();
		persistenceTrash = Collections.synchronizedList(new ArrayList<>());
		monitorInterval = defaultMonitorInterval;
		intervalsCount = defaultIntervalsCount;
	}
	
	public void add(@NonNull PersistenceInterface persistence) {
		put(persistence.hashKeys(), persistence);
	}
	
	public void put(String hashCode, @NonNull PersistenceInterface persistence) {
		persistenceTrash.remove(hashCode);
		persistenceMap.putIfAbsent(hashCode, persistence);
		persistenceTimeout.put(hashCode, intervalsCount);
		startMonitorThread();
	}
	
	public <T> List<T> find(Class<T> clazz, Predicate<T> filter) {
		return persistenceMap.values()
		                     .stream()
		                     .filter(clazz::isInstance)
		                     .map(clazz::cast)
		                     .filter(filter)
		                     .toList();
	}
	
	public @Nullable PersistenceInterface get(String hashCode) {
		return persistenceMap.get(hashCode);
	}
	
	public void remove(String hashCode) {
		persistenceTimeout.replace(hashCode, -1);
	}
	
	public boolean containsAny(Class<?> clazz) {
		return persistenceMap.values()
		                     .stream()
		                     .anyMatch(clazz::isInstance);
	}
	
	private void startMonitorThread() {
		if (monitorThread != null) return;
		monitorThread = Thread.ofVirtual()
		                      .name("Persistence Memory Monitor")
		                      .start(this::monitor);
	}
	
	private void monitor() {
		while (!(persistenceTimeout.isEmpty() || persistenceMap.isEmpty())) {
			
			persistenceMap.forEachKey(parallelismThreshold, key -> {
				if (key == null) return;
				if (persistenceTimeout.computeIfPresent(key, (k, v) -> {
					val i = v - 1;
					if (i >= 0) return i;
					return null;
				}) == null) persistenceTrash.add(key);
			});
			
			clearTrash();
			
			try {
				Thread.sleep(monitorInterval);
			} catch (InterruptedException e) {
				monitorThread = null;
				startMonitorThread();
			}
		}
		monitorThread = null;
	}
	
	private void clearTrash() {
		persistenceTrash.forEach(key -> {
			if (key == null) return;
			persistenceMap.remove(key);
			persistenceTimeout.remove(key);
		});
		persistenceTrash.clear();
	}
	
	public void clear() {
		persistenceMap.clear();
		persistenceTimeout.clear();
		persistenceTrash.clear();
	}
	
	public static Optional<PersistenceMemory> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(PersistenceMemory.class));
		} catch (NullPointerException e) {
			return java.util.Optional.empty();
		} catch (NoSuchBeanDefinitionException e) {
			LoggingWrapper.ROOT_LOGGER.fatal(e.getMessage());
			return java.util.Optional.empty();
		}
	}
	
	public String report() {
		return "Persistence Memory: Interval: %s x %s, Map: %s, Trash: %s.".formatted(monitorInterval, intervalsCount, persistenceMap.size(), persistenceTrash.size());
	}
	
}