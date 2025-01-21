package krystal.framework.database.implementation;

import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic settings and operations on databases
 */
@Service
@Getter
public final class QueryExecutor implements QueryExecutorInterface {
	
	private final Map<ProviderInterface, Properties> connectionProperties;
	private final Map<ProviderInterface, String> connectionStrings;
	
	private QueryExecutor() {
		connectionProperties = new ConcurrentHashMap<>();
		connectionStrings = new ConcurrentHashMap<>();
		
		// load props and urls
		this.loadProviders(KrystalFramework.getProvidersPool());
	}
	
	/**
	 * Static overload for {@link QueryExecutorInterface#loadProviders(List)}.
	 */
	public static void loadProviders(ProviderInterface... providers) {
		QueryExecutorInterface.getInstance().orElseThrow().loadProviders(Arrays.asList(providers));
	}
	
}