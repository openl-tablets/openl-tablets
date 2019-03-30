package org.openl.rules.testmethod.result;

public interface TestResultComparator {

    boolean isEqual(Object expectedResult, Object actualResult);
}
