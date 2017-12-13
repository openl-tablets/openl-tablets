package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.result.ComparedResult;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class BeanResultComparator implements TestResultComparator {
    private List<IOpenField> fields;
    private List<ComparedResult> comparisonResults = new ArrayList<ComparedResult>();

    BeanResultComparator(List<IOpenField> fields) {
        this.fields = fields;
    }

    public List<ComparedResult> getComparisonResults() {
        return comparisonResults;
    }

    public List<ComparedResult> getExceptionResults(Throwable actualResult, Object expectedResult) {
        if (comparisonResults.isEmpty()) {
            List<ComparedResult> results = new ArrayList<ComparedResult>();
            Throwable rootCause = ExceptionUtils.getRootCause(actualResult);
            if (rootCause == null) {
                rootCause = actualResult;
            }
            String actualFieldValue;
            if (rootCause instanceof OpenLUserRuntimeException) {
                actualFieldValue = ((OpenLUserRuntimeException) rootCause).getOriginalMessage();
            } else {
                actualFieldValue = rootCause.getMessage();
            }

            for (IOpenField field : fields) {
                ComparedResult fieldComparisonResults = new ComparedResult();
                fieldComparisonResults.setFieldName(field.getName());

                fieldComparisonResults.setActualValue(actualFieldValue);
                fieldComparisonResults.setExpectedValue(getFieldValueOrNull(expectedResult, field));

                // For BeanResultComparator expectedResult is complex object - that's why expectedResult
                // always doesn't equal to exception
                fieldComparisonResults.setStatus(TestStatus.TR_NEQ);

                results.add(fieldComparisonResults);
            }

            comparisonResults = results;
        }
        return comparisonResults;
    }

    public boolean isEqual(Object expectedResult, Object actualResult) {
        boolean success = true;
        comparisonResults = new ArrayList<>();

        for (IOpenField field : fields) {
            Object actualFieldValue = getFieldValueOrNull(actualResult, field);
            Object expectedFieldValue = getFieldValueOrNull(expectedResult, field);
            boolean equal = isCompare(actualFieldValue, expectedFieldValue, expectedResult, field);
            success = success && equal;

            ComparedResult fieldComparisonResults = new ComparedResult();
            fieldComparisonResults.setFieldName(field.getName());
            fieldComparisonResults.setActualValue(actualFieldValue);
            fieldComparisonResults.setExpectedValue(expectedFieldValue);
            fieldComparisonResults.setStatus(equal ? TestStatus.TR_OK : TestStatus.TR_NEQ);
            comparisonResults.add(fieldComparisonResults);
        }
        return success;

    }

    private boolean isCompare(Object actualFieldValue,
            Object expectedFieldValue,
            Object expectedResult,
            IOpenField field) {
        // Get delta for field if setted
        Double columnDelta = null;
        if (field instanceof PrecisionFieldChain) {
            if (((PrecisionFieldChain) field).hasDelta()) {
                columnDelta = ((PrecisionFieldChain) field).getDelta();
            }
        }

        try {
            // Additional convertation for spreadsheet. It is required for spreadsheet(StubSpreadsheet) created on
            // compilation state.
            if (expectedFieldValue != null && (actualFieldValue != null && expectedFieldValue
                .getClass() != actualFieldValue
                    .getClass()) && expectedResult instanceof SpreadsheetResult && expectedFieldValue instanceof String) {
                IString2DataConvertor convertor = String2DataConvertorFactory.getConvertor(actualFieldValue.getClass());
                expectedFieldValue = convertor.parse((String) expectedFieldValue, null);
            }
        } catch (Exception e) {
        }
        Class<?> clazz = field.getType().getInstanceClass();
        TestResultComparator comparator = TestResultComparatorFactory.getComparator(clazz, columnDelta);
        return comparator.isEqual(expectedFieldValue, actualFieldValue);
    }

    private Object getFieldValueOrNull(Object result, IOpenField field) {
        Object fieldValue = null;
        if (result != null) {
            try {
                IRuntimeEnv env = new SimpleVM().getRuntimeEnv();
                fieldValue = field.get(result, env);
            } catch (Exception ex) {
                fieldValue = ex;
            }
        }
        return fieldValue;
    }
}
