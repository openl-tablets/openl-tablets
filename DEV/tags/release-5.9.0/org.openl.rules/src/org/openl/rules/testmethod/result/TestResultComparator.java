package org.openl.rules.testmethod.result;

// TODO: refactor, use java.util.Comparator instead of own interface
public interface TestResultComparator {
    
    boolean compareResult(Object actualResult, Object expectedResult);
}
