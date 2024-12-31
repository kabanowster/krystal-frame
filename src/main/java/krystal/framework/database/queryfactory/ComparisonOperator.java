package krystal.framework.database.queryfactory;

public enum ComparisonOperator {
	IN("IN", null),
	NOT_IN("IN", "NOT "),
	MORE(">", null),
	LESS("<", null),
	MORE_EQUAL(">=", null),
	LESS_EQUAL("<=", null),
	EQUAL("=", null),
	NOT_EQUAL("=", "NOT "),
	BETWEEN("BETWEEN", null),
	NOT_BETWEEN("BETWEEN", "NOT "),
	LIKE("LIKE", null),
	NOT_LIKE("LIKE", "NOT ");
	
	public final String face;
	public final String prefix;
	
	ComparisonOperator(String face, String prefix) {
		this.face = face;
		this.prefix = prefix == null ? "" : prefix;
	}
}