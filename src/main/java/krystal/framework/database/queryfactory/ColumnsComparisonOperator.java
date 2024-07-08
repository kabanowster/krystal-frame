package krystal.framework.database.queryfactory;

public enum ColumnsComparisonOperator {
	IN("IN", null),
	NOT_IN("IN", "NOT "),
	MORE(">", null),
	LESS("<", null),
	MORE_EQUAL(">=", null),
	LESS_EQUAL("<=", null),
	EQUAL("=", null),
	NOT_EQUAL("=", "NOT "),
	BETWEEN("BETWEEN", null);
	
	public final String face;
	public final String prefix;
	
	ColumnsComparisonOperator(String face, String prefix) {
		this.face = face;
		this.prefix = prefix == null ? "" : prefix;
	}
}