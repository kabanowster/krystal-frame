package framework.database.queryfactory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@AllArgsConstructor
class WhereClauseOuterBlock {
	
	@Setter
	@NonNull
	private WhereClauseDelimiter delimiter;
	
	@Setter
	@NonNull
	private WhereClauseInnerBlock whereClause;
	
	@Override
	public String toString() {
		return String.format("%s(%s)", delimiter, whereClause);
	}
	
}