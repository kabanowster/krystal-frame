package apiserver.entities;

import apiserver.database.Tables;
import krystal.JSON;
import krystal.JSON.Flattison;
import krystal.Skip;
import krystal.Skip.SkipTypes;
import krystal.framework.database.abstraction.TableInterface;
import krystal.framework.database.persistence.Entity;
import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.persistence.annotations.*;
import krystal.framework.database.persistence.filters.PersistenceFilters;
import krystal.framework.database.persistence.filters.ValuesFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.val;
import org.json.JSONArray;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Memorized
@Flattison
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Stat implements Entity {
	
	private @Incremental @Key Integer id;
	private String name;
	private InputType input;
	private Boolean hiddenName;
	private @Column("[order]") Integer order;
	private Boolean mandatory;
	private @Skip(everythingElseBut = {SkipTypes.json}) Collection<Option> options;
	
	/*
	 * Constructors
	 */
	
	public Stat(int id) {
		this.id = id;
		load();
	}
	
	public Stat(String id) {
		this(Integer.parseInt(id));
	}
	
	
	/*
	 * Persistence definition
	 */
	
	@Reader
	private Stat(Integer id, String name, String input, @Nullable Boolean hiddenName, @Nullable Short order, @Nullable Boolean mandatory) {
		this.id = id;
		this.name = name;
		setInput(input);
		this.hiddenName = hiddenName != null && hiddenName;
		this.order = Optional.ofNullable(order).map(Short::intValue).orElse(null);
		this.mandatory = mandatory != null && mandatory;
	}
	
	@Override
	public TableInterface getTable() {
		return Tables.STATS;
	}
	
	/**
	 * Option.class:
	 * <dl>
	 *     <dt>radio, radioplus, combo, comboplus</dt>
	 *     <dd>selection element</dd>
	 *     <dt>valued_check</dt>
	 *     <dd>additional input fields, forming json string as value</dd>
	 * </dl>
	 */
	public enum InputType {
		field, textarea, chips, check, combo, multicombo, label
	}
	
	private void setInput(String input) {
		this.input = InputType.valueOf(input);
	}
	
	@SuppressWarnings("unchecked")
	private void setOptions(String options) {
		this.options = (Collection<Option>) JSON.into(new JSONArray(options), Set.class, Option.class);
	}
	
	/*
	 * RWR Methods
	 */
	
	@Reader
	public void readers() {
		val filter = PersistenceFilters.define()
		                               .value("stat", ValuesFilter.are(id))
		                               .set();
		options = Persistence.promiseAll(StatsOption.class, 0, filter).joinThrow().stream()
		                     .flatMap(stream -> stream.map(StatsOption::getOption)
		                                              .map(Option::new)
		                                              .filter(Option::noneIsNull))
		                     .toList();
	}
	
	@Writer
	private void writers() {
		if (options == null) return;
		val ids = options.stream().map(Option::getId).toList();
		val present = new ArrayList<Integer>(ids.size());
		val filters = PersistenceFilters.define()
		                                .value("stat", ValuesFilter.are(id))
		                                .memorized(false)
		                                .set();
		Persistence.promiseAll(StatsOption.class, filters).joinThrow().stream()
		           .flatMap(s -> s)
		           .forEach(so -> {
			           val opt = so.getOption();
			           if (ids.contains(opt)) {
				           present.add(opt);
			           } else {
				           so.delete();
			           }
		           });
		
		val remaining = ids.stream()
		                   .filter(i -> !present.contains(i))
		                   .toList();
		if (remaining.isEmpty()) return;
		
		val statement = Tables.STATS_OPTIONS.insert(StatsOption.Columns.values());
		remaining.forEach(s -> statement.values(id, s));
		statement.promise().joinThrow();
	}
	
	@Remover
	private void removers() {
		Persistence.promiseAll(StatsOption.class, PersistenceFilters.define()
		                                                            .value("stat", ValuesFilter.are(id))
		                                                            .memorized(false)
		                                                            .set())
		           .accept(opts -> opts.forEach(PersistenceInterface::delete))
		           .joinThrow();
	}
	
}