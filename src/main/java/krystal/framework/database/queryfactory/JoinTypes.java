package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.TableInterface;

/**
 * For SELF JOIN use {@link TableInterface#joinSelf(String, String, String...)}.
 */
public enum JoinTypes {
	LEFT, RIGHT, INNER, FULL
}