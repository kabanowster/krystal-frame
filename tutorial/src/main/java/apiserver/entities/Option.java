package apiserver.entities;

import krystal.JSON.Flattison;
import krystal.framework.database.persistence.Entity;
import krystal.framework.database.persistence.annotations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Memorized
@Flattison
@Table("statistics.options")
@AllArgsConstructor(onConstructor_ = {@Reader})
@NoArgsConstructor
@Getter
public class Option implements Entity {
	
	private @Incremental @Key Integer id;
	private String name;
	
	public Option(int id) {
		this.id = id;
		load();
	}
	
}