package org.openl.rules.testmethod;

import java.util.Map;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.DynamicObject;
import org.openl.util.Log;
import org.openl.util.formatters.IFormatter;
import org.openl.vm.IRuntimeEnv;

public class TestDescription {
    private final static String PRECISION_PARAM = "precision";

    private ParameterWithValueDeclaration[] executionParams;
    private IOpenMethod testedMethod;
    private DynamicObject testObject;
    private Map<String, Object> testTableProps = null;
    private int index;

    public TestDescription(IOpenMethod testedMethod, DynamicObject testObject) {
        this.testedMethod = testedMethod;
        this.testObject = testObject;
        executionParams = initExecutionParams();
    }

    public TestDescription(IOpenMethod testedMethod, DynamicObject testObject, Map<String, Object> params) {
        this(testedMethod, testObject);
        this.testTableProps = params;
    }

    public TestDescription(IOpenMethod testedMethod, Object[] arguments) {
        this(testedMethod, arguments, null, null);
    }

    public TestDescription(IOpenMethod testedMethod, Object[] arguments, Object expectedResult, String expectedError) {
        this(testedMethod, createTestObject(testedMethod, arguments, expectedResult, expectedError, null, null));
    }

    public TestDescription(IOpenMethod testedMethod,
            Object[] arguments,
            Object expectedResult,
            String expectedError,
            IRulesRuntimeContext context) {
        this(testedMethod, createTestObject(testedMethod, arguments, expectedResult, expectedError, context, null));
    }

    public TestDescription(IOpenMethod testedMethod,
            Object[] arguments,
            Object expectedResult,
            String expectedError,
            IRulesRuntimeContext context,
            String description) {
        this(testedMethod, createTestObject(testedMethod,
            arguments,
            expectedResult,
            expectedError,
            context,
            description));
    }

    public static DynamicObject createTestObject(IOpenMethod testedMethod,
            Object[] arguments,
            Object expectedResult,
            String expectedError,
            IRulesRuntimeContext context,
            String description) {
        // TODO should be created OpenClass like in TestSuiteMethod
        DynamicObject testObj = new DynamicObject();
        for (int i = 0; i < testedMethod.getSignature().getNumberOfParameters(); i++) {
            String paramName = testedMethod.getSignature().getParameterName(i);
            testObj.setFieldValue(paramName, arguments[i]);
        }
        if (description != null) {
            testObj.setFieldValue(TestMethodHelper.DESCRIPTION_NAME, context);
        }
        if (context != null) {
            testObj.setFieldValue(TestMethodHelper.CONTEXT_NAME, context);
        }
        if (expectedResult != null) {
            testObj.setFieldValue(TestMethodHelper.EXPECTED_RESULT_NAME, context);
        }
        if (expectedError != null) {
            testObj.setFieldValue(TestMethodHelper.EXPECTED_ERROR, context);
        }
        return testObj;
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    public DynamicObject getTestObject() {
        return testObject;
    }

    public ParameterWithValueDeclaration[] getExecutionParams() {
        return executionParams;
    }

    public String[] getParametersNames() {
        String[] names = new String[executionParams.length];
        for (int i = 0; i < executionParams.length; i++) {
            names[i] = executionParams[i].getName();
        }
        return names;
    }

    private OpenLArgumentsCloner cloner = new OpenLArgumentsCloner();

    public Object[] getArguments() {
        Object[] args = new Object[executionParams.length];
        for (int i = 0; i < args.length; i++) {
            Object value = executionParams[i].getValue();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (value != null) {
                    Thread.currentThread().setContextClassLoader(value.getClass().getClassLoader());
                }
                try {
                    args[i] = cloner.deepClone(value);
                } catch (RuntimeException e) {
                    Log.error("Failed to clone an argument \"{0}\". Original argument will be used.",
                        executionParams[i].getName());
                    args[i] = value;
                }
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
        return args;
    }

    protected ParameterWithValueDeclaration[] initExecutionParams() {
        ParameterWithValueDeclaration[] executionParams = new ParameterWithValueDeclaration[testedMethod.getSignature()
            .getNumberOfParameters()];
        for (int i = 0; i < executionParams.length; i++) {
            String paramName = testedMethod.getSignature().getParameterName(i);
            Object paramValue = testObject.getFieldValue(paramName);
            IOpenClass paramType = testedMethod.getSignature().getParameterType(i);
            executionParams[i] = new ParameterWithValueDeclaration(paramName,
                paramValue,
                paramType,
                IParameterDeclaration.IN);
        }
        return executionParams;
    }

    public TestUnit runTest(Object target, IRuntimeEnv env, long ntimes) {
        if (ntimes <= 0) {
            return runTest(target, env, 1);
        } else {
            Object res = null;
            Throwable exception = null;

            try {
                IRuntimeContext context = getRuntimeContext();

                IRuntimeContext oldContext = env.getContext();
                env.setContext(context);
                Object[] args = getArguments();
                for (int j = 0; j < ntimes; j++) {
                    res = testedMethod.invoke(target, args, env);
                }
                env.setContext(oldContext);
            } catch (Throwable t) {
                Log.error("Testing " + this, t);
                if (exception == null) {
                    exception = t;
                }
            }
            return exception == null ? new TestUnit(this, res, null) : new TestUnit(this, null, exception);
        }
    }

    public Object getArgumentValue(String paramName) {
        return testObject.getFieldValue(paramName);
    }

    public boolean hasDescription() {
        return testObject.containsField(TestMethodHelper.DESCRIPTION_NAME);
    }

    public String getDescription() {
        return (String) getArgumentValue(TestMethodHelper.DESCRIPTION_NAME);
    }

    public boolean isExpectedResultDefined() {
        return testObject.containsField(TestMethodHelper.EXPECTED_RESULT_NAME)
        // When all test cases contain empty (null) expected value
        || testObject.getType().getField(TestMethodHelper.EXPECTED_RESULT_NAME) != null;
    }

    public Object getExpectedResult() {
        return getArgumentValue(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public boolean isExpectedErrorDefined() {
        return testObject.containsField(TestMethodHelper.EXPECTED_ERROR);
    }

    public String getExpectedError() {
        return (String) getArgumentValue(TestMethodHelper.EXPECTED_ERROR);
    }

    public boolean isRuntimeContextDefined() {
        return testObject.containsField(TestMethodHelper.CONTEXT_NAME);
    }

    public IRulesRuntimeContext getRuntimeContext() {
        return (IRulesRuntimeContext) getArgumentValue(TestMethodHelper.CONTEXT_NAME);
    }

    @Override
    public String toString() {
        String description = getDescription();

        if (description == null) {
            if (testedMethod.getSignature().getNumberOfParameters() > 0) {
                String name = testedMethod.getSignature().getParameterName(0);
                Object value = testObject.getFieldValue(name);
                IFormatter formatter = FormattersManager.getFormatter(value);
                description = formatter.format(value);
            } else {
                description = "Run with no parameters";
            }
        }
        return description;
    }

    public Integer getTestTablePrecision() {
        if (this.testTableProps != null) {
            return this.testTableProps.containsKey(PRECISION_PARAM) ? Integer.parseInt(this.testTableProps.get(PRECISION_PARAM)
                                                                       .toString())
                                                                   : null;
        }

        return null;
    }

    /**
     * Returns an ID of the test case. The ID is get from _id_ column or generated on index base.
     */
    public String getId() {
        if (testObject.containsField(TestMethodHelper.TEST_ID)) {
            return String.valueOf(getArgumentValue(TestMethodHelper.TEST_ID));
        } else {
            return String.valueOf(index + 1);
        }

    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
