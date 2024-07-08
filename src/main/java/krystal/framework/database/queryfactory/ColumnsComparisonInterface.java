package krystal.framework.database.queryfactory;

/**
 * Used to pair columns with values in different query parts.
 *
 * @see ColumnSetValueComparison
 * @see ColumnToValueComparison
 */
@FunctionalInterface
public interface ColumnsComparisonInterface {
	
	String getComparison();
	
}