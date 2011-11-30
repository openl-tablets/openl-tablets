package org.openl.rules.calc.result;

import org.apache.commons.lang.ClassUtils;
import org.openl.rules.calc.SpreadsheetResult;

public class SpreadsheetResultHelper {
    private SpreadsheetResultHelper(){}
    
    public static boolean isSpreadsheetResult(Class<?> clazz) {
        return ClassUtils.isAssignable(clazz, SpreadsheetResult.class, false);
    }
}
