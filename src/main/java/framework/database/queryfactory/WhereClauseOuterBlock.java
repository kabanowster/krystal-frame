package framework.database.queryfactory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
class WhereClauseOuterBlock {
	
	@NonNull
	private WhereClauseDelimiter delimiter;
	
	@NonNull
	private WhereClauseInnerBlock whereClause;
	
	@Override
	public String toString() {
		return String.format("%s(%s)", delimiter, whereClause);
	}
	
}