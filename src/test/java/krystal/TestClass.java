package krystal;

import krystal.framework.KrystalFramework;
import krystal.framework.core.flow.implementation.Flows;
import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.logging.LoggingInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestClass implements LoggingInterface {
	
	static final ProviderInterface testServer = new ProviderInterface() {
		@Override
		public String name() {
			return "sqldev";
		}
		
		@Override
		public DBCDriverInterface dbcDriver() {
			return DBCDrivers.jdbcSQLServer;
		}
	};
	
	@BeforeAll
	static void loadBeforeTests() {
		KrystalFramework.selectDefaultImplementations();
		// KrystalFramework.setExposedDirPath("data");
		KrystalFramework.setDefaultProvider(testServer);
		KrystalFramework.frameSpringConsole(List.of(TestClass.class), "--loglvl=all");
		QueryExecutorInterface.getInstance().orElseThrow().loadProviders(List.of(testServer));
	}
	
	@AfterAll
	static void afterAllTests() {
		Flows.core.await(0);
	}
	
	@Test
	void generalTest() {
		PersistenceInterface.promiseAll(Machine.class)
		                    .map(s -> s.map(Machine::render).toList())
		                    .map(StringRenderer::renderMaps)
		                    .accept(this::logTest)
		                    .joinThrow();
		
	}
	
}