package krystal.framework.database.persistence.filters;

import krystal.framework.database.queryfactory.OrderByDeclaration;
import krystal.framework.database.queryfactory.WhereClause;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Basic SQL statement modifiers builder and holder.
 */
@Builder(builderMethodName = "define", buildMethodName = "set")
@Getter
public class StatementModifiers {
	
	private UnaryOperator<WhereClause> where;
	private Integer limit;
	private @Singular(value = "orderBy") List<OrderByDeclaration> orderBy;
	
}