package org.openl.rules.testmethod.result;

import java.lang.reflect.Array;

public class ArrayComparator implements TestResultComparator {

    @Override
    public boolean compareResult(Object actualResult, Object expectedResult) {
        int len = Array.getLength(actualResult);
        if (len != Array.getLength(expectedResult)) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            if (!compareResult(Array.get(actualResult, i), Array.get(expectedResult, i))) {
                return false;
            }
        }

        return true;
    }

}
