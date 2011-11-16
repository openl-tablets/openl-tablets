package org.openl.rules.testmethod.result;

public interface TestResultComparator {
    
    boolean compareResult(Object actualResult, Object expectedResult);
}
