package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.ForeignKeyColumnDescriptor;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.RowIdField;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.util.Log;

public class TestDescription {

    private ParameterWithValueDeclaration[] executionParams;
    private IOpenMethod testedMethod;
    private DynamicObject testObject;
    private int index;
    private ColumnDescriptor[] columnDescriptors;
    private List<IOpenField> fields = new ArrayList<>();

    public TestDescription(IOpenMethod testedMethod,
            DynamicObject testObject,
            List<IOpenField> fields,
            ColumnDescriptor[] columnDescriptors,
            IDataBase db) {
        this.testedMethod = testedMethod;
        this.testObject = testObject;
        this.fields = fields;
        this.columnDescriptors = columnDescriptors;
        executionParams = initExecutionParams(testedMethod, testObject, db, columnDescriptors);
    }

    public TestDescription(IOpenMethod testedMethod, Object[] arguments, IDataBase db) {
        this.testedMethod = testedMethod;
        this.testObject = createTestObject(testedMethod, arguments);
        executionParams = initExecutionParams(testedMethod, testObject, db, null);
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

    public Object[] getArguments(OpenLArgumentsCloner cloner) {
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

    private static ParameterWithValueDeclaration[] initExecutionParams(IOpenMethod testedMethod, DynamicObject testObject, IDataBase db, ColumnDescriptor[] columnDescriptors) {
        ParameterWithValueDeclaration[] executionParams = new ParameterWithValueDeclaration[testedMethod.getSignature()
                .getNumberOfParameters()];
        for (int i = 0; i < executionParams.length; i++) {
            String paramName = testedMethod.getSignature().getParameterName(i);
            Object paramValue = testObject.getFieldValue(paramName);
            IOpenClass paramType = testedMethod.getSignature().getParameterType(i);

            IOpenField keyField = getKeyField(paramName, paramType, paramValue, db, columnDescriptors);

            executionParams[i] = new ParameterWithValueDeclaration(paramName,
                    paramValue,
                    paramType,
                    keyField
            );
        }
        return executionParams;
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
                || testObject.getType() != null && testObject.getType().getField(TestMethodHelper.EXPECTED_RESULT_NAME) != null;
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

    public IRulesRuntimeContext getRuntimeContext(OpenLArgumentsCloner cloner) {
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

    protected static IOpenField getKeyField(String paramName, IOpenClass type, Object value, IDataBase db, ColumnDescriptor[] columnDescriptors) {
        if (value == null) {
            return null;
        }
        IOpenField foreignKeyField = null;
        if (columnDescriptors != null) {
            for (ColumnDescriptor columnDescriptor : columnDescriptors) {
                if (columnDescriptor == null) {
                    continue;
                }
                IdentifierNode[] fieldChainTokens = columnDescriptor.getFieldChainTokens();
                if (fieldChainTokens.length > 0 && fieldChainTokens[0].getIdentifier().equals(paramName)) {
                    // Found first column descriptor for needed parameter
                    if (columnDescriptor.isReference() && columnDescriptor instanceof ForeignKeyColumnDescriptor) {
                        // Foreign key to a data described in the Data Table
                        ForeignKeyColumnDescriptor descriptor = (ForeignKeyColumnDescriptor) columnDescriptor;
                        foreignKeyField = descriptor.getForeignKeyField(type, db);
                    } else {
                        // Test data is described in the current Test Table
                        if (fieldChainTokens.length > 1) {
                            // The field of a complex bean
                            IdentifierNode fieldName = fieldChainTokens[fieldChainTokens.length - 1];
                            foreignKeyField = type.getField(fieldName.getIdentifier());
                        }
                    }
                    break;
                }
            }
        }
        if (foreignKeyField == null) {
            // Couldn't find foreign key field in foreign Data Table or current Test Table - fallback to index field
            foreignKeyField = type ==null ? null : type.getIndexField();
        }

        return foreignKeyField;
    }
}
