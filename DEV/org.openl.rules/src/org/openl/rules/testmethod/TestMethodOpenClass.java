package org.openl.rules.testmethod;

import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.data.RowIdField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.DynamicObjectField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class TestMethodOpenClass extends ADynamicClass {

    public TestMethodOpenClass(String tableName, IOpenMethod testedMethod) {

        super(tableName + "TestClass", DynamicObject.class);

        init(testedMethod);
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        return new DynamicObject(this);
    }

    private IOpenMethod testedMethod;

    protected void init(IOpenMethod testedMethod) {

        this.testedMethod = testedMethod;

        addParameterFields(testedMethod);

        addExpectedResult(testedMethod);

        addDescription();

        addContext();

        addExpectedError();

        addTestId();
    }

    protected void addParameterFields(IOpenMethod testedMethod) {
        var parameterTypes = testedMethod.getSignature().getParameterTypes();

        for (var i = 0; i < parameterTypes.length; i++) {
            var name = testedMethod.getSignature().getParameterName(i);
            var parameterField = new DynamicObjectField(this, name, parameterTypes[i]);

            addField(parameterField);
        }
    }

    protected void addExpectedError() {
        var errorField = new DynamicObjectField(this,
                TestMethodHelper.EXPECTED_ERROR,
                new UserErrorOpenClass());
        addField(errorField);
    }

    protected void addContext() {
        var contextField = new DynamicObjectField(this,
                TestMethodHelper.CONTEXT_NAME,
                JavaOpenClass.getOpenClass(DefaultRulesRuntimeContext.class));
        addField(contextField);
    }

    protected void addDescription() {
        var descriptionField = new DynamicObjectField(this,
                TestMethodHelper.DESCRIPTION_NAME,
                JavaOpenClass.STRING);
        addField(descriptionField);
    }

    protected void addExpectedResult(IOpenMethod testedMethod) {
        var resultField = new DynamicObjectField(this,
                TestMethodHelper.EXPECTED_RESULT_NAME,
                testedMethod.getType());
        addField(resultField);
    }

    protected void addTestId() {
        var idField = new DynamicObjectField(this, RowIdField.ROW_ID, JavaOpenClass.STRING);
        addField(idField);
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }
}
