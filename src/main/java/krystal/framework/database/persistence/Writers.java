package krystal.framework.database.persistence;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.function.Supplier;

@Builder(builderMethodName = "define", buildMethodName = "set")
@Getter
@Accessors(fluent = true)
public class Writers {
	
	private @Singular Map<String, Supplier<Object>> writers;
	
	public static Writers empty() {
		return new Writers(Map.of());
	}
	
}