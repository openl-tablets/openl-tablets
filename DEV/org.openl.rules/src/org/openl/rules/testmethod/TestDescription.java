package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.data.RowIdField;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.runtime.IRuntimeContext;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DatatypeArrayElementField;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.ThisField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.vm.IRuntimeEnv;

public class TestDescription {
    private final static String PRECISION_PARAM = "precision";

    private ParameterWithValueDeclaration[] executionParams;
    private IOpenMethod testedMethod;
    private DynamicObject testObject;
    private Map<String, Object> testTableProps = null; // TODO Store testTablePrecision value instead of full map
    private int index;
    private ColumnDescriptor[] columnDescriptors;
    private List<IOpenField> fields;

    public TestDescription(IOpenMethod testedMethod, DynamicObject testObject, Map<String, Object> testTableProps, ColumnDescriptor[] columnDescriptors) {
        this.testedMethod = testedMethod;
        this.testObject = testObject;
        this.testTableProps = testTableProps;
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

    public Map<String, Object> getTestTableProps() {
        return testTableProps;
    }

    public List<IOpenField> getFields() {
        if (fields == null) {
            ColumnDescriptor[] descriptors = getColumnDescriptors();
            IOpenMethod testedMethod = getTestedMethod();
            Integer precision = null;
            if (testTableProps != null && testTableProps.containsKey(PRECISION_PARAM)) {
                precision = Integer.parseInt(testTableProps.get(PRECISION_PARAM).toString());
            }
            fields = createFieldsToTest(testedMethod, descriptors, precision);
        }
        return fields;
    }

    private static List<IOpenField> createFieldsToTest(IOpenMethod testedMethod, ColumnDescriptor[] descriptors, Integer testTablePrecision) {
        IOpenClass resultType = testedMethod.getType();
        List<IOpenField> fieldsToTest = new ArrayList<>();
        for (ColumnDescriptor columnDescriptor : descriptors) {
            if (columnDescriptor != null) {
                IdentifierNode[] nodes = columnDescriptor.getFieldChainTokens();
                if (nodes.length == 0 || !nodes[0].getIdentifier().startsWith(TestMethodHelper.EXPECTED_RESULT_NAME)) {
                    // skip empty or non-'_res_' columns
                    continue;
                }
                Integer fieldPrecision = testTablePrecision;
                if (nodes.length > 1 && nodes[nodes.length - 1].getIdentifier().matches(DataTableBindHelper.PRECISION_PATTERN)) {
                    // set the precision of the field
                    fieldPrecision = DataTableBindHelper.getPrecisionValue(nodes[nodes.length - 1]);
                    nodes = ArrayUtils.remove(nodes, nodes.length - 1);
                }

                if (columnDescriptor.isReference()) {
                    if (resultType.isSimple()) {
                        fieldsToTest.add(new ThisField(resultType));
                    } else if (resultType.isArray()) {
                        fieldsToTest.add(new ThisField(resultType));
                    } else {
                        fieldsToTest.addAll(resultType.getFields().values());
                    }
                } else {
                    IOpenField[] fieldSequence;
                    boolean resIsArray = nodes[0].getIdentifier().matches(DataTableBindHelper.ARRAY_ACCESS_PATTERN);
                    int startIndex = 0;
                    IOpenClass currentType = resultType;

                    if (resIsArray) {
                        startIndex = 1;
                        fieldSequence = new IOpenField[nodes.length];
                        IOpenField arrayField = new ThisField(resultType);
                        int arrayIndex = getArrayIndex(nodes[0]);
                        IOpenField arrayAccessField = new DatatypeArrayElementField(arrayField, arrayIndex);
                        if (arrayAccessField.getType().isArray()) {
                            currentType = arrayAccessField.getType().getComponentClass();
                        } else {
                            currentType = arrayAccessField.getType();
                        }
                        fieldSequence[0] = arrayAccessField;
                    } else {
                        fieldSequence = new IOpenField[nodes.length - 1];
                    }

                    for (int i = startIndex; i < fieldSequence.length; i++) {
                        boolean isArray = nodes[i + 1 - startIndex].getIdentifier()
                                .matches(DataTableBindHelper.ARRAY_ACCESS_PATTERN);
                        if (isArray) {
                            IOpenField arrayField = currentType.getField(getArrayName(nodes[i + 1 - startIndex]));
                            // Try process field as SpreadsheetResult
                            if (arrayField == null && currentType.equals(JavaOpenClass.OBJECT) && nodes[i + 1 - startIndex].getIdentifier()
                                    .matches(DataTableBindHelper.SPREADSHEETRESULTFIELD_PATTERN)) {
                                SpreadsheetResultOpenClass spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(SpreadsheetResult.class);
                                arrayField = spreadsheetResultOpenClass.getField(getArrayName(nodes[i + 1 - startIndex]));
                            }
                            int arrayIndex = getArrayIndex(nodes[i + 1 - startIndex]);
                            IOpenField arrayAccessField = new DatatypeArrayElementField(arrayField, arrayIndex);
                            fieldSequence[i] = arrayAccessField;
                        } else {
                            fieldSequence[i] = currentType.getField(nodes[i + 1 - startIndex].getIdentifier());
                            if (fieldSequence[i] == null) {
                                // Try process field as SpreadsheetResult
                                SpreadsheetResultOpenClass spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(SpreadsheetResult.class);
                                IOpenField openField = spreadsheetResultOpenClass.getField(nodes[i + 1 - startIndex].getIdentifier());
                                if (openField != null) {
                                    fieldSequence[i] = openField;
                                }
                            }
                        }

                        if (fieldSequence[i].getType().isArray() && isArray) {
                            currentType = fieldSequence[i].getType().getComponentClass();
                        } else {
                            currentType = fieldSequence[i].getType();
                        }
                    }
                    if (fieldSequence.length == 0) {
                        fieldSequence = new IOpenField[] { new ThisField(resultType) };
                    }
                    if (fieldPrecision != null) {
                        fieldsToTest.add(new PrecisionFieldChain(currentType, fieldSequence, fieldPrecision));
                    } else if (fieldSequence.length > 1) {
                        fieldsToTest.add(new FieldChain(currentType, fieldSequence));
                    } else {
                        fieldsToTest.add(fieldSequence[0]);
                    }
                }
            }
        }
        return fieldsToTest;
    }

    private static int getArrayIndex(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        String txtIndex = fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]"));

        return Integer.parseInt(txtIndex);
    }

    private static String getArrayName(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        return fieldName.substring(0, fieldName.indexOf("["));
    }
}
