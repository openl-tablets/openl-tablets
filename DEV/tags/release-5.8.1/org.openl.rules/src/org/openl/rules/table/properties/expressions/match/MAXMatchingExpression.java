package org.openl.rules.table.properties.expressions.match;


public class MAXMatchingExpression extends AMatchingExpression {

	public static final String OPERATION_NAME = "MAX";

	public boolean isContextAttributeExpression() {
		return true;
	}

	public MAXMatchingExpression(IMatchingExpression matchingExpression) {
		super(OPERATION_NAME, matchingExpression);
	}

	public String getOperation() {
		return ((AMatchingExpression) getContextAttributeExpression())
				.getOperation();
	}
	
	@Override
	public String getCodeExpression(String param) {
		return getContextAttributeExpression().getCodeExpression(param);
	}
}
