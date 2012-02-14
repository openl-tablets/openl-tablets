package org.openl.rules.testmethod.result;

public class ComparableResultComparator implements TestResultComparator {

    @SuppressWarnings("unchecked")
    public boolean compareResult(Object actualResult, Object expectedResult) {        
        return ((Comparable<Object>) actualResult).compareTo(expectedResult) == 0;
    }
    
}
