package krystal.framework.database.persistence.filters;

import krystal.framework.database.queryfactory.ComparisonOperator;
import lombok.val;

import java.util.Arrays;
import java.util.function.Predicate;

public record ValuesFilter(ComparisonOperator operator, Object... values) implements Predicate<Object> {
	
	public static ValuesFilter are(Object... values) {
		return new ValuesFilter(ComparisonOperator.EQUAL, values);
	}
	
	public static ValuesFilter areNot(Object... values) {
		return new ValuesFilter(ComparisonOperator.NOT_EQUAL, values);
	}
	
	public static ValuesFilter are(ComparisonOperator operator, Object... values) {
		return new ValuesFilter(operator, values);
	}
	
	@Override
	public boolean test(Object value) {
		if (values.length == 0) return false;
		switch (operator) {
			case EQUAL, IN -> {
				for (val v : values) if (v == value || value.equals(v)) return true;
				return false;
			}
			case MORE, LESS, MORE_EQUAL, LESS_EQUAL, BETWEEN -> {
				val dValue = Double.parseDouble(String.valueOf(value));
				val dVals = Arrays.stream(values).map(v -> Double.valueOf(String.valueOf(v))).sorted().toList();
				switch (operator) {
					case MORE -> {
						for (val v : dVals) {
							if (dValue <= v) return false;
						}
						return true;
					}
					case LESS -> {
						for (val v : dVals) {
							if (dValue >= v) return false;
						}
						return true;
					}
					case MORE_EQUAL -> {
						for (val v : dVals) {
							if (dValue < v) return false;
						}
						return true;
					}
					case LESS_EQUAL -> {
						for (val v : dVals) {
							if (dValue > v) return false;
						}
						return true;
					}
					case BETWEEN -> {
						return dValue >= dVals.getFirst() && dValue <= dVals.getLast();
					}
				}
			}
			case NOT_EQUAL, NOT_IN -> {
				for (val v : values) if (v == value || value.equals(v)) return false;
				return true;
			}
		}
		return false;
	}
	
}