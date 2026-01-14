package apiserver;

import apiserver.database.Providers;
import apiserver.servlets.Servlets;
import krystal.ConsoleView;
import krystal.framework.KrystalFramework;
import krystal.framework.core.flow.implementation.Flows;
import krystal.framework.database.implementation.ConnectionPool;
import krystal.framework.database.persistence.PersistenceMemory;
import krystal.framework.tomcat.TomcatProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Log4j2
@ComponentScan
@Configuration
public class Main {
	
	public static void main(String[] args) {
		KrystalFramework.setProvidersPool(List.of(Providers.primary, Providers.secondary));
		KrystalFramework.setDefaultProvider(Providers.primary);
		KrystalFramework.setExposedDirPath("data");
		KrystalFramework.setProvidersPropertiesDir("");
		ConnectionPool.setDefaultConfig(ConnectionPool.createConfig(c -> {
			c.setMaximumPoolSize(50);
			c.setConnectionTimeout(175 * 1000);
			c.setMaxLifetime(60 * 60 * 1000);
			c.setValidationTimeout(15 * 1000);
			c.setIdleTimeout(10 * 60 * 1000);
		}));
		PersistenceMemory.setDefaultMonitorInterval(15 * 1000);
		PersistenceMemory.setDefaultIntervalsCount(4);
		ConsoleView.setCharactersLimit(250000);
		KrystalFramework.frameSpringConsole(List.of(Main.class), args);
		
		KrystalFramework.startTomcatServer(
				TomcatProperties.builder()
				                .connectionTimeout(5 * 60 * 1000)
				                .servlet(Servlets.statisticsServlet())
				                .servlet(Servlets.customCommentsServlet())
				                .build()
		);
		
		// prevent self-collapsing
		Flows.core.await(0);
	}
	
}