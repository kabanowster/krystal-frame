package krystal.framework.database.persistence.annotations;

import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.QueryResultInterface;
import krystal.framework.database.persistence.ColumnsMap;
import krystal.framework.database.persistence.PersistenceInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * By default, each row of declared {@link PersistenceInterface#getTable() persitence table}, represents a single persisted object. With this annotation present, the fields are constructed from provided static returns of
 * {@link PivotColumn @PivotColumn} and {@link ValuesColumn @ValuesColumn}. Basically, un-pivoting the underlying {@link PersistenceInterface#getQuery() query}, before passing to the
 * {@link Reader constructor}. In addition and unless {@link ReadOnly @ReadOnly}, {@link Key @Keys} fields are mandatory (treated as group-by fields) for writing operations.
 *
 * @see PersistenceInterface#mapQueryResult(QueryResultInterface, Class)
 * @see ColumnsMap
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Vertical {
	
	/**
	 * Static return of {@link ColumnInterface} of {@link PersistenceInterface#getTable()} that describes fields names.
	 */
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface PivotColumn {
	
	}
	
	/**
	 * Static return of {@link ColumnInterface} of {@link PersistenceInterface#getTable()} that describes fields values.
	 */
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface ValuesColumn {
	
	}
	
	/**
	 * Optional static return of {@link List} of {@link ColumnInterface} that the underlying query will be un-pivoted into.
	 *
	 * @see QueryResultInterface#unpivot(ColumnInterface, ColumnInterface, ColumnInterface...)
	 */
	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	@interface UnpivotToColumns {
	
	}
	
}