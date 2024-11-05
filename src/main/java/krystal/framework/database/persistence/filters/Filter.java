package krystal.framework.database.persistence.filters;

import krystal.framework.database.persistence.Persistence;
import krystal.framework.database.persistence.PersistenceInterface;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.Builder;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Class used to define filtering on {@link Field Fields} for {@link Persistence} loading.
 * Equivalent of {@link WhereClause} for SQL statements.
 *
 * @see Persistence#promiseAll(Class, long, Filter)
 */
@Slf4j
@Builder
public record Filter(ConditionalDelimiter condition, @Singular Map<String, ValuesFilter> fields) implements Predicate<Object> {
	
	public Filter {
		condition = condition == null ? ConditionalDelimiter.AND : condition;
	}
	
	public boolean test(Object object) {
		val conditionsToMatch = (long) fields.size();
		val conditionsMatching = Arrays.stream(object.getClass()
		                                             .getDeclaredFields())
		                               .filter(Field::trySetAccessible)
		                               .filter(f -> fields.containsKey(f.getName()))
		                               .filter(f -> {
			                               try {
				                               val value = f.get(object);
				                               return fields.get(f.getName()).test(value);
			                               } catch (IllegalAccessException e) {
				                               log.error("Filter field access failed", e);
				                               return false;
			                               }
		                               }).count();
		
		switch (condition) {
			case AND -> {
				return conditionsMatching == conditionsToMatch;
			}
			case OR -> {
				return conditionsMatching > 0;
			}
		}
		return false;
	}
	
	public <T> Predicate<T> toPredicate() {
		return this::test;
	}
	
	public StatementModifiers toStatementModifier(Class<?> clazz) {
		return StatementModifiers.define()
		                         .where((w) -> {
			                         PersistenceInterface.getFieldsToColumnsMap(clazz, null)
			                                             .columns().entrySet().stream()
			                                             .filter(e -> fields.containsKey(e.getKey().getName()))
			                                             .map(e -> {
				                                             val vf = fields.get(e.getKey().getName());
				                                             return e.getValue().is(vf.operator(), vf.values());
			                                             }).forEach(w::andWhere);
			                         return w;
		                         }).set();
	}
	
}