import krystal.framework.KrystalFramework;
import krystal.framework.core.flow.implementation.Flows;
import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.implementation.Q;
import krystal.framework.logging.LoggingInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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
		KrystalFramework.primaryInitialization("--logtofile=no --loglvl=all");
		KrystalFramework.startSpringCore(List.of(TestClass.class));
		QueryExecutorInterface.getInstance().orElseThrow().loadProviders(List.of(testServer));
		// KrystalFramework.startConsole();
	}
	
	@AfterAll
	static void afterAllTests() {
		Flows.core.await(0);
	}
	
	@Test
	void generalTest() {
		try {
			Q.t("MASZYNY").select().promise()
			 .map(Objects::toString)
			 .accept(this::logTest)
			 .joinExceptionally();
		} catch (ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
	
}