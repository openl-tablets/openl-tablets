package org.openl.rules.testmethod;

import java.util.ArrayList;

public final class TestUtils {

    private TestUtils() {
    }

    public static ParameterWithValueDeclaration[] getContextParams(TestSuite test, TestDescription testCase) {
        var params = new ArrayList<ParameterWithValueDeclaration>();

        var context = testCase.getRuntimeContext();
        var testMethod = test.getTestSuiteMethod();
        if (testMethod != null) {
            for (var i = 0; i < testMethod.getColumnsCount(); i++) {
                var columnName = testMethod.getColumnName(i);
                if (columnName != null && columnName.startsWith(TestMethodHelper.CONTEXT_NAME)) {

                    Object value = context != null ? context
                            .getValue(columnName.replace(TestMethodHelper.CONTEXT_NAME + ".", "")) : null;

                    params.add(new ParameterWithValueDeclaration(columnName, value));
                }
            }
        }

        return params.toArray(ParameterWithValueDeclaration.EMPTY_ARRAY);
    }
}
