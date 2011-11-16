package org.openl.rules.testmethod.result;

import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.SpreadsheetResult;
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
        
        if (actualResult.getClass().isArray() && expectedResult.getClass().isArray()) {
            return new ArrayComparator();
        }
        
//        if (ClassUtils.isAssignable(actualResult.getClass(), SpreadsheetResult.class, false)) {
//            return new BeanResultComparator(fieldsToCompare);
//        }
        
        return new DefaultComparator();
    }
    
    public static TestResultComparator getBeanComparator(Object actualResult, Object expectedResult, List<String> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
