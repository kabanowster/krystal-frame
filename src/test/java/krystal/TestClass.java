package krystal;

import krystal.VirtualPromise.ExceptionsHandler;
import krystal.framework.KrystalFramework;
import krystal.framework.core.ConsoleProgress;
import krystal.framework.core.flow.implementation.Flows;
import krystal.framework.database.abstraction.DBCDriverInterface;
import krystal.framework.database.abstraction.ProviderInterface;
import krystal.framework.database.abstraction.QueryExecutorInterface;
import krystal.framework.database.implementation.DBCDrivers;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.logging.LoggingInterface;
import lombok.val;
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
		KrystalFramework.frameSpringConsole(null, List.of(TestClass.class), "--loglvl=all");
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
	
	@Test
	void monitorTest() {
		val other = VirtualPromise.run(() -> logTest("This is holding promise"))
		                          .setOnHold()
		                          .thenRun(() -> logTest("Holding promise is free again"));
		
		PersistenceInterface.promiseAll(Machine.class)
		                    .catchThrow()
		                    .map(s -> s.map(Machine::render).toList())
		                    .map(StringRenderer::renderMaps)
		                    .apply(r -> {
			                    log().fatal("Will wait now for the other guy");
			                    return r;
		                    })
		                    .mirror(() -> other)
		                    .accept(this::logTest);
		
		VirtualPromise.run(() -> {
			logTest("Setting holder timeout");
			for (int i = 1; i <= 5; i++) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				logTest("elapsed time: " + i);
			}
			other.resumeNext();
		});
	}
	
	@Test
	void joinTest() {
		val other = VirtualPromise.run(() -> logTest("This primary promise"))
		                          .monitor(_ -> {
			                          for (int i = 0; i < 10; i++) {
				                          if (i == 5) {
					                          throw new RuntimeException("motherfuckr!");
				                          }
				                          try {
					                          Thread.sleep(1000);
				                          } catch (InterruptedException _) {
				                          }
				                          logTest("this is some logging: " + i);
			                          }
		                          })
		                          .catchExceptions(new ExceptionsHandler(log()::fatal))
		                          .thenRun(() -> logTest("Primary promise ended successfully"))
		                          .catchRun(_ -> logTest("This one was caught later"));
		
	}
	
	@Test
	void progressBarTest() throws InterruptedException {
		val progress = new ConsoleProgress(20).level("info").render();
		log().warn("Progress start under content.");
		for (int i = 0; i <= progress.getTarget(); i += 10) {
			Thread.sleep(1000);
			log().info("this is some logging: {}", i);
			progress.update(i);
		}
		log().warn("Progress end, dispose after 2s.");
		Thread.sleep(2000);
		progress.dispose();
		log().warn("Progress disposed.");
	}
	
}