package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.context.IRulesRuntimeContext;

public final class TestUtils {
    
    private TestUtils() {
    }

    public static ParameterWithValueDeclaration[] getContextParams(TestSuite test, TestDescription testCase) {
        List<ParameterWithValueDeclaration> params = new ArrayList<>();

        TestSuiteMethod testMethod = test.getTestSuiteMethod();
        IRulesRuntimeContext context = testCase.getRuntimeContext(test.getArgumentsCloner());

        for (int i = 0; i < testMethod.getColumnsCount(); i++) {
            String columnName = testMethod.getColumnName(i);
            if (columnName != null && columnName.startsWith(TestMethodHelper.CONTEXT_NAME)) {

                Object value = context != null ? context
                    .getValue(columnName.replace(TestMethodHelper.CONTEXT_NAME + ".", "")) : null;

                params.add(new ParameterWithValueDeclaration(columnName, value));
            }
        }

        return params.toArray(new ParameterWithValueDeclaration[params.size()]);
    }
}
