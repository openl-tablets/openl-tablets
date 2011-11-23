package org.openl.rules.testmethod.result;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.util.StringTool;

public class BeanResultComparator implements TestResultComparator {
    private List<ComparedResult> fieldsToCompare;
    
    public BeanResultComparator(List<String> fieldsToCompare) {
        if (fieldsToCompare != null) {
            this.fieldsToCompare = new ArrayList<ComparedResult>();
            for (String fieldToCompare : fieldsToCompare) {
                ComparedResult cr = new ComparedResult();
                cr.setFieldName(fieldToCompare);
                this.fieldsToCompare.add(cr);
            }            
        } else {
            throw new IllegalArgumentException("Fields for comparing cannot be null");
        }
    }
    
    public List<ComparedResult> getFieldsToCompare() {
        return new ArrayList<ComparedResult>(fieldsToCompare);
    }

    public boolean compareResult(Object actualResult, Object expectedResult) {
        boolean success = true;
        for (ComparedResult fieldToCompare : fieldsToCompare) {
            String fieldName = fieldToCompare.getFieldName();
            Object actualFieldValue = getFieldValue(actualResult, fieldName);
            Object expectedFieldValue = getFieldValue(expectedResult, fieldName);
            
            TestResultComparator comparator = TestResultComparatorFactory.getComparator(actualFieldValue, expectedFieldValue);
            boolean compare = comparator.compareResult(actualFieldValue, expectedFieldValue);
            
            fieldToCompare.setActualValue(actualFieldValue);
            fieldToCompare.setExpectedValue(expectedFieldValue);
            
            if (!compare) {
                fieldToCompare.setStatus(TestStatus.TR_NEQ);
                success = false;
            } else {
                fieldToCompare.setStatus(TestStatus.TR_OK);
            }
        }
        return success;
    }
    
    private Object getFieldValue(Object target, String fieldName) {
        Object res = null;
        Class<?> targetClass = target.getClass();
        Method method;
        try {
            method = targetClass.getMethod(StringTool.getGetterName(fieldName), new Class<?>[0]);
            res = method.invoke(target, new Object[0]);
        } catch (Exception e1) {
            String message = String.format("Cannot get value for field %s", fieldName);
            throw new OpenlNotCheckedException(message, e1);
        }
        return res;
    }

}
