package org.openl.rules.testmethod;

import org.openl.base.INamedThing;
import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.runtime.IRuntimeContext;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.Log;
import org.openl.util.print.Formatter;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends AMethod implements IMemberMetaInfo, IBenchmarkableMethod {

    private String tableName;
    private IOpenMethod testedMethod;
    private TestMethodBoundNode boundNode;
    private IOpenClass methodBasedClass; 
    
    public TestSuiteMethod(String tableName, IOpenMethod testedMethod, TestMethodBoundNode boundNode) {
        super(TestMethodHelper.makeHeader(tableName, testedMethod));
    
        this.tableName = tableName;
        this.testedMethod = testedMethod;
        this.boundNode = boundNode;
    }

    public TestMethodBoundNode getBoundNode() {
        return boundNode;
    }

    public String[] unitName() {
        return new String[] { "Test Unit", "Test Units" };
    }

    public String getBenchmarkName() {
        return "Test " + testedMethod.getName();
    }

    public BindingDependencies getDependencies() {
        return new BindingDependencies();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public int getNumberOfTests() {

        Object testArray = boundNode.getField().getTable().getDataArray();
        DynamicObject[] dd = (DynamicObject[]) testArray;
        
        return dd.length;
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) getSyntaxNode()).getUri();
    }

    public ISyntaxNode getSyntaxNode() {
        return boundNode.getSyntaxNode();
    }

    public String[] getTestDescriptions() {
        
        Object testArray = boundNode.getField().getTable().getDataArray();

        DynamicObject[] dd = (DynamicObject[]) testArray;

        String[] descriptions = new String[dd.length];

        for (int i = 0; i < descriptions.length; i++) {
            
            String description = (String) dd[i].getFieldValue(TestMethodHelper.DESCRIPTION_NAME);
            
            if (description == null) {
                if (testedMethod.getSignature().getNumberOfParameters() > 0) {
                    String name = testedMethod.getSignature().getParameterName(0);
                    Object value = dd[i].getFieldValue(name);
                    description = Formatter.format(value, INamedThing.REGULAR, new StringBuffer()).toString();
                } else {
                    description = "Run with no parameters";
                }
            }
            
            descriptions[i] = description;
        }

        return descriptions;
    }

    public synchronized IOpenClass getMethodBasedClass() {

        if (methodBasedClass == null) {
            methodBasedClass = new TestMethodOpenClass(tableName, testedMethod);
        }

        return methodBasedClass;
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return invoke(target, params, env, -1);
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env, int unitId) {
        return invokeBenchmark(target, params, env, 1, unitId);
    }

    public Object invokeBenchmark(Object target, Object[] params, IRuntimeEnv env, int ntimes) {
        return invokeBenchmark(target, params, env, ntimes, -1);
    }

    public Object invokeBenchmark(Object target, Object[] params, IRuntimeEnv env,
            int ntimes, int unitId) {

        Object testArray = boundNode.getField().get(target, env);

        DynamicObject[] testInstances = (DynamicObject[]) testArray;

        IOpenClass dclass = getMethodBasedClass();
        IMethodSignature msign = testedMethod.getSignature();
        IOpenClass[] mpars = msign.getParameterTypes();

        TestUnitsResults tres = new TestUnitsResults(this);

        int unitStart = unitId > -1 ? unitId : 0;
        int unitStop = unitId > -1 ? (unitStart + 1) : testInstances.length;

        for (int i = unitStart; i < unitStop; i++) {

            Object[] mpvals = new Object[mpars.length];

            DynamicObject currentTest = testInstances[i];

            for (int j = 0; j < mpars.length; j++) {
                IOpenField f = dclass.getField(msign.getParameterName(j), true);
                mpvals[j] = f.get(currentTest, env);
            }

            try {
                Object res = null;

                for (int j = 0; j < ntimes; j++) {
                    IOpenField contextField = dclass.getField(TestMethodHelper.CONTEXT_NAME);
                    IRuntimeContext context = (IRuntimeContext) contextField.get(currentTest, env);

                    IRuntimeContext oldContext = env.getContext();
                    env.setContext(context);

                    res = testedMethod.invoke(target, mpvals, env);

                    env.setContext(oldContext);
                }

                tres.addTestUnit(currentTest, res, null);
            } catch (Throwable t) {
                Log.error("Testing " + currentTest, t);
                tres.addTestUnit(currentTest, null, t);
            }

        }

        return tres;
    }

    public boolean isRunmethod() {
        TableSyntaxNode tsn = (TableSyntaxNode) getSyntaxNode();
        return ITableNodeTypes.XLS_RUN_METHOD.equals(tsn.getType());
    }
    
    /**
     * Indicates if test method has any row rules for testing target table.
     * Finds it by field that contains {@link TestMethodHelper#EXPECTED_RESULT_NAME}
     * @return
     * 
     * TODO: rename it. it is difficult to understand what is it doing 
     */
    public boolean isRunmethodTestable() {
        // gets the data from rows that have test parameters.
        Object testArray = boundNode.getField().getTable().getDataArray();

        DynamicObject[] testArrayDynamicObj = (DynamicObject[]) testArray;

        for (int i = 0; i < testArrayDynamicObj.length; i++) {
            if (testArrayDynamicObj[i].containsField(TestMethodHelper.EXPECTED_RESULT_NAME)) {
                return true;
            }
        }

        return false;
    }

    public int nUnitRuns() {
        return getNumberOfTests();
    }

    public Object run(int tid, Object target, IRuntimeEnv env, int ntimes) {

        Object testArray = boundNode.getField().get(target, env);

        DynamicObject[] dd = (DynamicObject[]) testArray;

        IOpenClass dclass = getMethodBasedClass();
        IMethodSignature msign = testedMethod.getSignature();
        IOpenClass[] mpars = msign.getParameterTypes();

        DynamicObject currentTest = dd[tid];
        Object[] mpvals = new Object[mpars.length];

        for (int j = 0; j < mpars.length; j++) {
            IOpenField f = dclass.getField(msign.getParameterName(j), true);
            mpvals[j] = f.get(currentTest, env);
        }

        Object res = null;

        for (int i = 0; i < ntimes; i++) {
            IOpenField contextField = dclass.getField(TestMethodHelper.CONTEXT_NAME);
            IRuntimeContext context = (IRuntimeContext) contextField.get(currentTest, env);

            IRuntimeContext oldContext = env.getContext();
            env.setContext(context);

            res = testedMethod.invoke(target, mpvals, env);

            env.setContext(oldContext);
        }

        return res;
    }

}
