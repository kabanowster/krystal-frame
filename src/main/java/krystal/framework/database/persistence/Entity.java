package krystal.framework.database.persistence;

import krystal.CheckImportantFieldsInterface;
import krystal.Skip;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.tomcat.KrystalServlet;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;

/**
 * A subject for persistence.
 *
 * <h3>Essential elements:</h3>
 * <dl>
 *     <dt>{@link NoArgsConstructor} - a must be</dt>
 *     <dd>Constructor without arguments.</dd>
 *     <dt>{@link TableInterface} - a must be</dt>
 *     <dd>Implement by using {@link Table} annotation or overriding {@link PersistenceInterface#getTable()} method.</dd>
 *     <dt>Constructor({@link String})</dt>
 *     <dd>Defining constructor for single {@link String} argument is a must for {@link KrystalServlet} single GET mapping (i.e. "/api/v1/entity/123", where "123" is passed as argument).</dd>
 *     <dt>{@link ExplicitImportance}</dt>
 *     <dd>The {@link KrystalServlet} single object GET implementation validates the returned instance with {@link CheckImportantFieldsInterface#noneIsNull()} method. This option can limit fields being taken to predicate (up to none).</dd>
 *     <dt>{@link ColumnInterface}</dt>
 *     <dd>Each {@link Field} of a class is treated as a database column with name exact as field's. To declare a different column name, either use {@link Column} on the field, or declare {@link ColumnsMapping}.</dd>
 *     <dt>{@link Key}</dt>
 *     <dd>{@link Field Fields} marked will be treated as object's identifiers within database. Can overlap primary keys, but not necessarily.<br/>See also: {@link SeparateKeys}, {@link Incremental}</dd>
 *     <dt>{@link Vertical}</dt>
 *     <dd>A way to unpivot underlying {@link Loader} query before passing to object constructor.</dd>
 *     <dt>{@link Skip}</dt>
 *     <dd>{@link Field} marked will be excluded from persistence operations.</dd>
 *     <dt>See also: {@link Loader}, {@link Filter}, {@link Reader}, {@link Writer}, {@link ReadOnly}, {@link PersistenceMemory}</dt>
 * </dl>
 *
 * @see PersistenceInterface
 */
public interface Entity extends PersistenceInterface, CheckImportantFieldsInterface {

}