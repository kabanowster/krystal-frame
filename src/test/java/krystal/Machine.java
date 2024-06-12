package krystal;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.implementation.Q;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.annotations.ColumnsMapping;
import krystal.framework.database.persistence.annotations.Filter;
import krystal.framework.database.persistence.annotations.Loader;
import krystal.framework.database.persistence.annotations.Reader;
import krystal.framework.database.queryfactory.ColumnOperators;
import krystal.framework.database.queryfactory.SelectStatement;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.function.Function;

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
		return WhereClause.filter(s -> s.where(Columns.linia.is(ColumnOperators.Less, 9000)));
	}
	
	@Override
	public String toString() {
		return "[%s %s]".formatted(linia, nazwa);
	}
	
}