package framework.database.queryfactory;

public enum ColumnOperators {
	In("IN", null),
	notIn("IN", "NOT "),
	More(">", null),
	Less("<", null),
	MoreEqual(">=", null),
	LessEqual("<=", null),
	Equal("=", null),
	notEqual("=", "NOT ");
	
	public final String face;
	public final String prefix;
	
	ColumnOperators(String face, String prefix) {
		this.face = face;
		this.prefix = prefix == null ? "" : prefix;
	}
}