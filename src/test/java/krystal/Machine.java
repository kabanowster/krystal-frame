package krystal;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.Q;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.annotations.ColumnsMapping;
import krystal.framework.database.persistence.annotations.Filter;
import krystal.framework.database.persistence.annotations.Loader;
import krystal.framework.database.persistence.annotations.Reader;
import krystal.framework.database.queryfactory.ColumnsComparisonOperator;
import krystal.framework.database.queryfactory.SelectStatement;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
public class Machine implements PersistenceInterface {
	
	private Integer linia;
	private String nazwa;
	
	@Reader
	private Machine(Integer linia, String nazwa) {
		this.linia = linia;
		this.nazwa = nazwa;
	}
	
	@Override
	public TableInterface getTable() {
		return Q.t("MASZYNY");
	}
	
	@Loader
	@ColumnsMapping
	@Getter
	public enum Columns implements ColumnInterface {
		linia("linia"),
		nazwa("nazwa");
		final String sqlName;
		
		Columns(String sqlName) {
			this.sqlName = sqlName;
		}
	}
	
	@Filter
	private Function<SelectStatement, WhereClause> filter() {
		return s -> s.where(Columns.linia.is(ColumnsComparisonOperator.LESS, 9000));
	}
	
	@Override
	public String toString() {
		return "[%s %s]".formatted(linia, nazwa);
	}
	
	public Map<String, String> render() {
		return Arrays.stream(getClass().getDeclaredFields())
		             .filter(Field::trySetAccessible)
		             .collect(Collectors.toMap(
				             Field::getName,
				             f -> {
					             try {
						             return String.valueOf(f.get(this));
					             } catch (IllegalAccessException e) {
						             throw new RuntimeException(e);
					             }
				             }
		             ));
	}
	
	private void writeNazwa() {
		logTest("Machine Name: " + nazwa);
	}
	
}