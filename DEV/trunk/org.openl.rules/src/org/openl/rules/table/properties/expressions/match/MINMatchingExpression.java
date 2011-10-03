package org.openl.rules.table.properties.expressions.match;


public class MINMatchingExpression extends AMatchingExpression {
        
    public static final String OPERATION_NAME = "MIN";
   
    public boolean isContextAttributeExpression() {
        return true;
    }
    
    public MINMatchingExpression(IMatchingExpression matchingExpression) {
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

