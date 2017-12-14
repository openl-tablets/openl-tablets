package org.openl.rules.testmethod;

import java.util.List;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.RowIdField;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.util.Log;
import org.openl.vm.IRuntimeEnv;

public class TestDescription {

    private ParameterWithValueDeclaration[] executionParams;
    private IOpenMethod testedMethod;
    private DynamicObject testObject;
    private int index;
    private ColumnDescriptor[] columnDescriptors;
    private List<IOpenField> fields;

    public TestDescription(IOpenMethod testedMethod, DynamicObject testObject, List<IOpenField> fields, ColumnDescriptor[] columnDescriptors) {
        this.testedMethod = testedMethod;
        this.testObject = testObject;
        this.fields = fields;
        this.columnDescriptors = columnDescriptors;
        executionParams = initExecutionParams(testedMethod, testObject);
    }

    public TestDescription(IOpenMethod testedMethod, Object[] arguments) {
        this.testedMethod = testedMethod;
        this.testObject = createTestObject(testedMethod, arguments);
        executionParams = initExecutionParams(testedMethod, testObject);
    }

    private static DynamicObject createTestObject(IOpenMethod testedMethod, Object[] arguments) {
        // TODO should be created OpenClass like in TestSuiteMethod
        DynamicObject testObj = new DynamicObject();
        IMethodSignature signature = testedMethod.getSignature();
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String paramName = signature.getParameterName(i);
            testObj.setFieldValue(paramName, arguments[i]);
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

    private static ParameterWithValueDeclaration[] initExecutionParams(IOpenMethod testedMethod, DynamicObject testObject) {
        ParameterWithValueDeclaration[] executionParams = new ParameterWithValueDeclaration[testedMethod.getSignature()
                .getNumberOfParameters()];
        for (int i = 0; i < executionParams.length; i++) {
            String paramName = testedMethod.getSignature().getParameterName(i);
            Object paramValue = testObject.getFieldValue(paramName);
            IOpenClass paramType = testedMethod.getSignature().getParameterType(i);
            executionParams[i] = new ParameterWithValueDeclaration(paramName,
                    paramValue,
                    paramType
            );
        }
        return executionParams;
    }

    @SuppressWarnings("unchecked")
    public TestUnit runTest(Object target, IRuntimeEnv env, long ntimes) {
        if (ntimes <= 0) {
            return runTest(target, env, 1);
        } else {
            Object res = null;
            Throwable exception = null;
            IRuntimeContext oldContext = env.getContext();
            long time;
            long start = System.nanoTime(); // Initialization here is needed if exception is thrown
            long end;
            try {
                IRuntimeContext context = getRuntimeContext();
                env.setContext(context);
                Object[] args = getArguments();
                // Measure only actual test run time
                start = System.nanoTime();
                for (int j = 0; j < ntimes; j++) {
                    res = testedMethod.invoke(target, args, env);
                }
                end = System.nanoTime();
            } catch (Throwable t) {
                end = System.nanoTime();
                exception = t;
            } finally {
                env.setContext(oldContext);
            }
            time = end - start;
            return exception == null ? new TestUnit(this, res, time) : new TestUnit(this, exception, time);
        }
    }

    private Object getArgumentValue(String paramName) {
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
        IRulesRuntimeContext context = (IRulesRuntimeContext) getArgumentValue(TestMethodHelper.CONTEXT_NAME);
        
        if (context == null) {
            return RulesRuntimeContextFactory.buildRulesRuntimeContext();
        }
        
        try {
            return cloner.deepClone(context);
        } catch (Exception e) {
            Log.error("Failed to clone context. Original context will be used.");
            return context;
        }
    }

    /**
     * Returns an ID of the test case. The ID is get from _id_ column or generated on index base.
     */
    public String getId() {
        if (hasId()) {
            return String.valueOf(getArgumentValue(RowIdField.ROW_ID));
        } else {
            return String.valueOf(index + 1);
        }
    }

    public boolean hasId() {
        return testObject.containsField(RowIdField.ROW_ID);
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public ColumnDescriptor[] getColumnDescriptors() {
        return columnDescriptors;
    }

    public List<IOpenField> getFields() {
        return fields;
    }
}
