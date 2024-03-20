package krystal.framework.database.implementation;

import krystal.framework.core.PropertiesAndArguments;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Basic settings and operations on databases
 */
//@Service
@Getter
public final class QueryExecutor implements QueryExecutorInterface {
	
	private static @Setter Providers defaultProvider;
	private final Map<ProviderInterface, Properties> connectionProperties;
	private final Map<ProviderInterface, String> connectionStrings;
	
	private QueryExecutor() {
		connectionProperties = Collections.synchronizedMap(new HashMap<>());
		connectionStrings = Collections.synchronizedMap(new HashMap<>());
		
		// load props and urls
		loadProviderProperties(Providers.values());
	}
	
	@Override
	public ProviderInterface getDefaultProvider() {
		return PropertiesAndArguments.provider.value().map(p -> Providers.valueOf((String) p))
		                                      .or(() -> Optional.ofNullable(defaultProvider))
		                                      .orElse(Providers.sqlserver);
	}
	
}