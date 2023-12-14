package krystal.framework.database.queryfactory;

/**
 * Used to pair columns with values in different query parts.
 *
 * @see ColumnSetPair
 * @see ColumnIsPair
 */
@FunctionalInterface
public interface ColumnsPairingInterface {
	
	String pairTogether();
	
}