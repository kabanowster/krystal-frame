package apiserver.database;

import krystal.framework.database.abstraction.TableInterface;

public enum Tables implements TableInterface {
	//OPTIONS("options"),
	STATS("stats"),
	STATS_OPTIONS("stats_options");
	
	private final String sqlName;
	
	Tables(String sqlName) {
		this.sqlName = sqlName;
	}
	
	@Override
	public String getSqlName() {
		return "statistics." + sqlName;
	}
}