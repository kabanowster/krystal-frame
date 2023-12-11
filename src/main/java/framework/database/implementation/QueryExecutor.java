package framework.database.implementation;

import framework.KrystalFramework;
import framework.core.PropertiesAndArguments;
import framework.database.abstraction.ProviderInterface;
import framework.database.abstraction.QueryExecutorInterface;
import framework.database.abstraction.QueryResultInterface;
import krystal.Tools;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic settings and operations on databases
 *
 * @author Wiktor Kabanow
 */
@Service
@Getter
public final class QueryExecutor implements QueryExecutorInterface {
	
	private final Map<Providers, Properties> connectionProperties;
	private final Map<Providers, String> connectionStrings;
	
	private QueryExecutor() {
		connectionProperties = Collections.synchronizedMap(new HashMap<>());
		connectionStrings = Collections.synchronizedMap(new HashMap<>());
		
		// load props and urls
		Stream.of(Providers.values()).forEach(provider -> {
			var props = new Properties();
			try {
				props.load(Tools.getResource(KrystalFramework.getExposedDirPath(), provider.toString() + ".properties").openStream());
				connectionProperties.put(provider, props);
				
				// TODO error throw if mandatory missing?
				connectionStrings.put(
						provider,
						provider.jdbcDriver().getConnectionStringBase()
								+ props.entrySet().stream()
								       .filter(e -> Stream.of(MandatoryProperties.values()).map(Enum::toString).anyMatch(m -> m.equals(e.getKey().toString())))
								       .sorted((e1, e2) -> e2.getKey().toString().compareTo(e1.getKey().toString()))
								       .map(e -> e.getValue().toString())
								       .collect(Collectors.joining("/"))
				);
				
			} catch (IOException ex) {
				log().fatal(String.format("!!! IO exception while loading '%s' provider properties file!", provider));
			}
		});
		
	}
	
	@Override
	public QueryResultInterface process(ResultSet rs) {
		return new QueryResult(rs);
	}
	
	@Override
	public ProviderInterface getDefaultProvider() {
		return Providers.valueOf((String) PropertiesAndArguments.provider.value().orElse(Providers.sqlserver.toString()));
	}
	
}