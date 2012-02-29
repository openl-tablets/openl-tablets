package org.openl.rules.testmethod.result;

import java.util.List;

import org.openl.rules.helpers.NumberUtils;

public class TestResultComparatorFactory {
    
    private TestResultComparatorFactory(){}
    
    public static TestResultComparator getComparator(Object actualResult, Object expectedResult) {
        if (NumberUtils.isFloatPointNumber(actualResult)) { 
            return new NumberComparator();
        } 
        
        if (actualResult instanceof Comparable) {
            return new ComparableResultComparator();
        }
        
        if (actualResult != null && expectedResult != null) {
            if (actualResult.getClass().isArray() && expectedResult.getClass().isArray()) {
                return new ArrayComparator();
            }
        }
         
        return new DefaultComparator();
    }
    
    public static TestResultComparator getBeanComparator(Object actualResult, Object expectedResult, List<String> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
