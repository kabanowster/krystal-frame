package krystal.framework.database.implementation;

import com.opencsv.bean.CsvToBeanBuilder;
import krystal.Tools;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import lombok.val;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@UtilityClass
@Log4j2
public class CSV {
	
	public <T> Stream<T> stream(Class<T> clazz, String... path) {
		val src = Tools.getResource(path);
		return src.map(url -> {
			try (val reader = Files.newBufferedReader(Path.of(url.toURI()))) {
				return new CsvToBeanBuilder<T>(reader).withType(clazz).build().parse().stream();
			} catch (IOException | URISyntaxException e) {
				throw new RuntimeException(e);
			}
			
		}).orElse(Stream.empty());
	}
	
	public void into(Path path) {
		try (val writer = Files.newBufferedWriter(path)) {
			// TODO csv write implementation
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}