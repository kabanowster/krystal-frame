package krystal.framework.database.persistence;

import krystal.framework.KrystalFramework;
import krystal.framework.database.persistence.annotations.Fresh;
import krystal.framework.database.persistence.annotations.Memorized;
import krystal.framework.database.persistence.filters.PersistenceFilters;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Each {@link PersistenceInterface} object loaded are saved in memory for quick access.
 * <p>
 *
 * @see Memorized
 * @see Fresh
 */
@Log4j2
public class PersistenceMemory {
	
	/**
	 * Interval (cycle) in {@code ms} at which the {@link #monitorThread} checks the memory.
	 */
	private static @Setter @Getter int defaultMonitorInterval = 1000;
	/**
	 * Number of {@link #monitorThread} cycles objects stay in memory.
	 */
	private static @Setter @Getter int defaultIntervalsCount = 3;
	
	/**
	 * @see ConcurrentHashMap#forEach(long, BiConsumer)
	 */
	private static @Setter @Getter int parallelismThreshold = 50;
	
	/**
	 * Doing the job of monitoring the objects status in memory, moving to and clearing the trash.
	 */
	private final AtomicReference<Thread> monitorThread;
	private final ReentrantLock persistenceLock;
	private final ConcurrentHashMap<String, PersistenceInterface> persistenceMap;
	private final ConcurrentHashMap<String, Integer> persistenceTimeout;
	private final List<String> persistenceTrash;
	/**
	 * @see #defaultMonitorInterval
	 */
	private @Setter @Getter int monitorInterval;
	/**
	 * @see #defaultIntervalsCount
	 */
	private @Setter @Getter int intervalsCount;
	
	public PersistenceMemory() {
		monitorThread = new AtomicReference<>();
		persistenceLock = new ReentrantLock();
		persistenceMap = new ConcurrentHashMap<>();
		persistenceTimeout = new ConcurrentHashMap<>();
		persistenceTrash = new CopyOnWriteArrayList<>();
		monitorInterval = defaultMonitorInterval;
		intervalsCount = defaultIntervalsCount;
	}
	
	/**
	 * @see #put(String, PersistenceInterface, int)
	 */
	public void put(@NonNull PersistenceInterface persistence) {
		put(persistence.hashKeys(), persistence, intervalsCount);
	}
	
	/**
	 * Put a new entry to the memory or replace (update) existing one.
	 *
	 * @param intervalsCount
	 * 		Overrides {@link #defaultIntervalsCount} and {@link #intervalsCount}
	 */
	public void put(String hashCode, @NonNull PersistenceInterface persistence, int intervalsCount) {
		persistenceTrash.remove(hashCode);
		persistenceMap.put(hashCode, persistence);
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
		try {
			while (!persistenceLock.tryLock()) {
				Thread.sleep(100);
			}
			
			if (monitorThread.get() == null) {
				monitorThread.set(Thread.ofVirtual()
				                        .name("Persistence Memory Monitor")
				                        .start(this::monitor));
				log.debug("Persistence Memory Monitor started.");
			}
			
			persistenceLock.unlock();
		} catch (Exception e) {
			log.fatal("Persistence Memory Monitor", e);
			monitorThread.set(null);
			startMonitorThread();
		}
	}
	
	private void monitor() {
		while (!(persistenceTimeout.isEmpty() || persistenceMap.isEmpty())) {
			
			persistenceMap.forEachKey(parallelismThreshold, key -> {
				if (key == null) return;
				
				if (persistenceTimeout.computeIfPresent(key, (k, v) -> {
					if (v < 0) return null;
					if (key.contains("@Memorized")) return v;
					return v - 1;
				}) == null) persistenceTrash.add(key);
			});
			
			clearTrash();
			
			try {
				Thread.sleep(monitorInterval);
			} catch (InterruptedException e) {
				monitorThread.set(null);
				startMonitorThread();
			}
		}
		monitorThread.set(null);
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
	
	public void clear(Class<?> clazz) {
		persistenceMap.entrySet()
		              .stream()
		              .filter(e -> clazz.isInstance(e.getValue()))
		              .map(Entry::getKey)
		              .forEach(this::remove);
	}
	
	public void clear(Class<?> clazz, PersistenceFilters filters) {
		persistenceMap.entrySet()
		              .stream()
		              .filter(e -> clazz.isInstance(e.getValue()))
		              .filter(e -> filters.test(e.getValue()))
		              .map(Entry::getKey)
		              .forEach(this::remove);
	}
	
	public static Optional<PersistenceMemory> getInstance() {
		try {
			return Optional.of(KrystalFramework.getSpringContext().getBean(PersistenceMemory.class));
		} catch (NullPointerException | NoSuchBeanDefinitionException e) {
			return java.util.Optional.empty();
		}
	}
	
	public String report() {
		return "Persistence Memory: Interval: %s x %s, Map: %s, Trash: %s.".formatted(monitorInterval, intervalsCount, persistenceMap.size(), persistenceTrash.size());
	}
	
}