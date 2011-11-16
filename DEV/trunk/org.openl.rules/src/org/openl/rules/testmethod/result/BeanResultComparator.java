package org.openl.rules.testmethod.result;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openl.util.StringTool;

public class BeanResultComparator implements TestResultComparator {
    
    private List<String> fieldsToCompare;
    
    private List<String> failedFields;
    
    public BeanResultComparator(List<String> fieldsToCompare) {
        if (fieldsToCompare != null) {
            this.fieldsToCompare = new ArrayList<String>(fieldsToCompare);            
        } else {
            throw new IllegalArgumentException("Fields for comparing cannot be null");
        }
        this.failedFields = new ArrayList<String>();
    }
    
    public boolean compareResult(Object actualResult, Object expectedResult) {
        for (String fieldToCompare : fieldsToCompare) {
            Object actualFieldValue = getFieldValue(actualResult, fieldToCompare);
            Object expectedFieldValue = getFieldValue(expectedResult, fieldToCompare);
            
            TestResultComparator comparator = TestResultComparatorFactory.getComparator(actualFieldValue, expectedFieldValue);
            boolean compare = comparator.compareResult(actualFieldValue, expectedFieldValue);
            if (!compare) {
                failedFields.add(fieldToCompare);
            }
        }
        return failedFields.isEmpty();
    }
    
    private Object getFieldValue(Object target, String fieldName) {
        Object res = null;
        Class<?> targetClass = target.getClass();
        Method method;
        try {
            method = targetClass.getMethod(StringTool.getGetterName(fieldName), new Class<?>[0]);
            res = method.invoke(target, new Object[0]);
        } catch (Exception e1) {
            // TODO: log error
        }
        return res;
    }

}
