package org.openl.rules.testmethod.result;

public class DefaultComparator implements TestResultComparator {

    public boolean compareResult(Object actualResult, Object expectedResult, Double delta) {
    	if (actualResult == null) {
    		return actualResult == expectedResult;
    	} else {
    		return actualResult.equals(expectedResult);
    	}
        
    }

}
