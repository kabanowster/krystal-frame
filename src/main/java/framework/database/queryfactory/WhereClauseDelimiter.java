package framework.database.queryfactory;

public enum WhereClauseDelimiter {
	AND, OR, NULL;
	
	@Override
	public String toString() {
		if (this == NULL)
			return "";
		
		return String.format(" %s ", name());
	}
}