package org.openl.rules.testmethod.result;

import org.openl.rules.helpers.NumberUtils;
import org.openl.util.math.MathUtils;

public class NumberComparator implements TestResultComparator {
    
    public boolean compareResult(Object actualResult, Object expectedResult) {
        Double result = NumberUtils.convertToDouble(actualResult);
        Double doubleResult = NumberUtils.convertToDouble(expectedResult);
        
        return MathUtils.eq(result.doubleValue(), doubleResult.doubleValue());
    }

}
