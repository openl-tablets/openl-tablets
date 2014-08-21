package org.openl.rules.testmethod.result;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLRuntimeException;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.types.IOpenField;
import org.openl.util.StringTool;

public class BeanResultComparator implements TestResultComparator {
    private List<String> fieldsToCompare;
    private List<ComparedResult> comparisonResults = new ArrayList<ComparedResult>();

    public BeanResultComparator(List<String> fieldsToCompare) {
        if (fieldsToCompare != null) {
            this.fieldsToCompare = fieldsToCompare;
        } else {
            throw new IllegalArgumentException("Fields for comparing cannot be null");
        }
    }

    public List<String> getFieldsToCompare() {
        return fieldsToCompare;
    }

    public List<ComparedResult> getComparisonResults() {
        return comparisonResults;
    }

    public boolean compareResult(Object actualResult, Object expectedResult, Double delta) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        } else {
            comparisonResults = new ArrayList<ComparedResult>();
            boolean success = true;
            for (String fieldToCompare : fieldsToCompare) {
                Double columnDelta = delta;
                ComparedResult fieldComparisonResults = new ComparedResult();
                fieldComparisonResults.setFieldName(fieldToCompare);

                Object actualFieldValue = null;
                try {
                    actualFieldValue = getFieldValue(actualResult, fieldToCompare);
                } catch (OpenLRuntimeException e) {
                    actualFieldValue = null;
                } catch (NullPointerException e) {
                    actualFieldValue = null;
                }
                Object expectedFieldValue = null;
                try {
                    expectedFieldValue = getFieldValue(expectedResult, fieldToCompare);
                } catch (OpenLRuntimeException e) {
                    actualFieldValue = null;
                } catch (NullPointerException e) {
                    actualFieldValue = null;
                }

                if (this instanceof OpenLBeanResultComparator) {
                    IOpenField field = ((OpenLBeanResultComparator) this).getField(fieldToCompare);

                    // Get delta for field if setted
                    if (field instanceof PrecisionFieldChain) {
                        if (((PrecisionFieldChain) field).hasDelta()) {
                            columnDelta = ((PrecisionFieldChain) field).getDelta();
                        }
                    }
                }
                boolean compare = false;
                try{
                    // Additional convertation for spreadsheet. It is required for spreadsheet(StubSpreadsheet) created on compilation state.
                    if (expectedFieldValue != null && (actualFieldValue != null && expectedFieldValue.getClass() != actualFieldValue.getClass()) && expectedResult instanceof SpreadsheetResult && expectedFieldValue instanceof String) {
                        IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(actualFieldValue.getClass());
                        expectedFieldValue = convertor.parse((String) expectedFieldValue, null);
                    }
                    TestResultComparator comparator = TestResultComparatorFactory.getComparator(actualFieldValue,
                        expectedFieldValue);
                    compare = comparator.compareResult(actualFieldValue, expectedFieldValue, columnDelta);
                }catch(Exception e){
                }
                
                fieldComparisonResults.setActualValue(actualFieldValue);
                fieldComparisonResults.setExpectedValue(expectedFieldValue);

                if (!compare) {
                    fieldComparisonResults.setStatus(TestStatus.TR_NEQ);
                    success = false;
                } else {
                    fieldComparisonResults.setStatus(TestStatus.TR_OK);
                }
                comparisonResults.add(fieldComparisonResults);
            }
            return success;
        }

    }

    protected Object getFieldValue(Object target, String fieldName) {
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
