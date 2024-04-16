package krystal.framework.database.implementation;

import krystal.framework.KrystalFramework;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Basic settings and operations on databases
 */
@Service
@Getter
public final class QueryExecutor implements QueryExecutorInterface {
	
	private final Map<ProviderInterface, Properties> connectionProperties;
	private final Map<ProviderInterface, String> connectionStrings;
	
	private QueryExecutor() {
		connectionProperties = Collections.synchronizedMap(new HashMap<>());
		connectionStrings = Collections.synchronizedMap(new HashMap<>());
		
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