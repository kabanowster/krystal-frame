package krystal.framework.database.queryfactory;

import krystal.framework.database.abstraction.ColumnInterface;

public record ColumnPair(ColumnInterface first, ColumnOperators is, ColumnInterface second) implements ColumnsPairingInterface {
	
	@Override
	public String pairTogether() {
		return "%s %s %s".formatted(first.getSqlName(), is.face, second.getSqlName());
	}
	
	@Override
	public String toString() {
		return pairTogether();
	}
	
	public static ColumnPair of(ColumnInterface first, ColumnOperators is, ColumnInterface second) {
		return new ColumnPair(first, is, second);
	}
	
}