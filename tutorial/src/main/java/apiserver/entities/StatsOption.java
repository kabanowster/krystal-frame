package apiserver.entities;

import apiserver.database.Tables;
import krystal.JSON.Flattison;
import krystal.framework.database.abstraction.ColumnInterface;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.persistence.Entity;
import krystal.framework.database.persistence.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Fresh
@Flattison
@AllArgsConstructor(onConstructor_ = {@Reader})
@NoArgsConstructor
@Getter
public class StatsOption implements Entity {
	
	private @Incremental @Key Integer id;
	private Integer stat;
	private Integer option;
	
	public StatsOption(String id) {
		this.id = Integer.parseInt(id);
		load();
	}
	
	/*
	 * Persistence definition
	 */
	
	@Override
	public TableInterface getTable() {
		return Tables.STATS_OPTIONS;
	}
	
	@ColumnsMapping
	@Getter
	public enum Columns implements ColumnInterface {
		stat("stat"),
		option("[option]");
		private final String sqlName;
		
		Columns(String sqlName) {
			this.sqlName = sqlName;
		}
	}
	
}