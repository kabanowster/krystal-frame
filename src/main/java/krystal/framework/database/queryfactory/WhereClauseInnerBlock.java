package krystal.framework.database.queryfactory;

import krystal.Tools;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Getter
public class WhereClauseInnerBlock {
	
	private final Set<ColumnsPairingInterface> columnIsPairs = new HashSet<>();
	@Setter
	private WhereClauseDelimiter delimiter;
	
	public WhereClauseInnerBlock(WhereClauseDelimiter delimiter, ColumnsPairingInterface... columnIsPairs) {
		this.delimiter = delimiter;
		setPairs(columnIsPairs);
	}
	
	public void setPairs(ColumnsPairingInterface... columnIsPairs) {
		this.columnIsPairs.addAll(Stream.of(columnIsPairs).toList());
	}
	
	@Override
	public String toString() {
		return Tools.concat(delimiter.toString(), columnIsPairs.stream());
	}
	
}