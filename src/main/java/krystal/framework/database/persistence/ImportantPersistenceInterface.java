package krystal.framework.database.persistence;

import krystal.CheckImportantFieldsInterface;

/**
 * Merge of {@link PersistenceInterface} and {@link CheckImportantFieldsInterface}.
 */
public interface ImportantPersistenceInterface extends PersistenceInterface, CheckImportantFieldsInterface {

}