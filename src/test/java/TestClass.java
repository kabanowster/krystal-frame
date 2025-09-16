import krystal.framework.KrystalFramework;
import krystal.framework.core.flow.implementation.Flows;
import krystal.framework.logging.LoggingInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TestClass implements LoggingInterface {
	
	@BeforeAll
	static void setup() {
		KrystalFramework.frameSpringConsole(null, List.of(), "--logtofile=no --loglvl=all");
	}
	
	@AfterAll
	static void coreWait() {
		Flows.core.await(0);
	}
	
	@Test
	void cleanRun() {
		logTest("Is running...");
	}
	
}