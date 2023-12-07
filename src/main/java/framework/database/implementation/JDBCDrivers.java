package framework.database.implementation;

import framework.database.abstraction.JDBCDriverInterface;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Getter
public enum JDBCDrivers implements JDBCDriverInterface {
	sqlserver("jdbc:sqlserver://"),
	as400("jdbc:as400://");
	
	private final String connectionStringBase;
	
	JDBCDrivers(String connectionStringBase) {
		this.connectionStringBase = connectionStringBase;
	}
	
}