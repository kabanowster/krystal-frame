package krystal.framework.database.queryfactory;

public enum QueryType {
	SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, CALL, EXEC, UNDEFINED;
	
	static public QueryType[] CUDs() {
		return new QueryType[] {
				INSERT, UPDATE, DELETE
		};
	}
}