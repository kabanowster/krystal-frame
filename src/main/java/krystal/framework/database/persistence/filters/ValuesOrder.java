package krystal.framework.database.persistence.filters;

import krystal.framework.database.queryfactory.OrderByDirection;
import lombok.val;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record ValuesOrder(OrderByDirection direction, String name) {
	
	public static ValuesOrder asc(String name) {
		return new ValuesOrder(OrderByDirection.ASC, name);
	}
	
	public static ValuesOrder desc(String name) {
		return new ValuesOrder(OrderByDirection.DESC, name);
	}
	
	public static <T> Stream<T> sort(List<T> objects, List<ValuesOrder> orderBy, Class<T> clazz) {
		val fields = Arrays.stream(clazz.getDeclaredFields())
		                   .filter(Field::trySetAccessible)
		                   .toList();
		
		Map<T, Map<String, Object>> values = new HashMap<>(objects.size());
		objects.forEach(obj -> {
			val vals = new HashMap<String, Object>(fields.size());
			fields.forEach(field -> {
				try {
					vals.put(field.getName(), field.get(obj));
				} catch (IllegalAccessException _) {
				}
			});
			values.put(obj, vals);
		});
		
		return objects.stream()
		              .sorted((a, b) -> {
			              val aVals = values.get(a);
			              val bVals = values.get(b);
			              
			              for (var vo : orderBy) {
				              val direction = switch (vo.direction) {
					              case ASC -> 1;
					              case DESC -> -1;
				              };
				              
				              val av = aVals.get(vo.name());
				              val bv = bVals.get(vo.name());
				              
				              var result = 0;
				              
				              if (av == null) result = -1;
				              if (bv == null) result = 1;
				              
				              if (result == 0) {
					              // noinspection rawtypes
					              if (av instanceof Comparable ac && bv instanceof Comparable bc) {
						              // noinspection unchecked
						              result = ac.compareTo(bc);
					              } else {
						              result = Integer.compare(av.hashCode(), bv.hashCode());
					              }
				              }
				              
				              if (result != 0) return result * direction;
			              }
			              return 0;
		              });
	}
	
}