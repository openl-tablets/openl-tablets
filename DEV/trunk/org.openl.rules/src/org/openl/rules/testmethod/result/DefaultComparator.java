package org.openl.rules.testmethod.result;

public class DefaultComparator implements TestResultComparator {

    @Override
    public boolean compareResult(Object actualResult, Object expectedResult) {
        return actualResult.equals(expectedResult);
    }

}
