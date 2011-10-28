package org.openl.rules.testmethod;

import java.util.List;
import java.util.Set;

import org.openl.binding.BindingDependencies;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.runtime.IRuntimeContext;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.util.Log;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends ExecutableRulesMethod implements IBenchmarkableMethod {

    private String tableName;
    private IOpenMethod testedMethod;
    private IOpenClass methodBasedClass;
    
    public TestSuiteMethod(String tableName, IOpenMethod testedMethod, IOpenMethodHeader header, TestMethodBoundNode boundNode) {
        super(header, boundNode);
    
        this.tableName = tableName;
        this.testedMethod = testedMethod;
        initProperties(getSyntaxNode().getTableProperties());
    }
    
    @Override
    public TestMethodBoundNode getBoundNode() {
        return (TestMethodBoundNode)super.getBoundNode();
    }

    public String[] unitName() {
        return new String[] { "Test Unit", "Test Units" };
    }

    public String getBenchmarkName() {
        return "Test " + testedMethod.getName();
    }

    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        
        updateDependency(bindingDependencies);
        
        return bindingDependencies;
    }    

    private void updateDependency(BindingDependencies bindingDependencies) {
        IOpenMethod testedMethod = getTestedMethod();
        if (testedMethod instanceof ExecutableRulesMethod || testedMethod instanceof OpenMethodDispatcher) {
            bindingDependencies.addMethodDependency(testedMethod, getBoundNode());
        }        
    }

    public int getNumberOfTests() {

        Object testArray = getBoundNode().getField().getData();
        DynamicObject[] dd = (DynamicObject[]) testArray;
        
        return dd.length;
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public TestDescription[] getTestDescriptions() {
        
        Object testArray = getBoundNode().getField().getData();

        DynamicObject[] dd = (DynamicObject[]) testArray;

        TestDescription[] descriptions = new TestDescription[dd.length];

        for (int i = 0; i < descriptions.length; i++) {
            descriptions[i] = new TestDescription(testedMethod, dd[i]);
        }

        return descriptions;
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        int columnIndex = getBoundNode().getTable().getColumnIndex(columnTechnicalName);
        if (columnIndex >= 0) {
            return getBoundNode().getTable().getColumnDisplay(columnIndex);
        } else {
            return null;
        }
    }

    public synchronized IOpenClass getMethodBasedClass(List<IdentifierNode[]> columnIdentifiers) {

        if (methodBasedClass == null) {
            methodBasedClass = TestMethodFactory.getTestMethodOpenClass(testedMethod, tableName, columnIdentifiers);
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

        Object testArray = getBoundNode().getField().get(target, env);

        DynamicObject[] testInstances = (DynamicObject[]) testArray;

        IOpenClass dclass = getMethodBasedClass(null);
        IMethodSignature msign = testedMethod.getSignature();
        IOpenClass[] mpars = msign.getParameterTypes();

        TestUnitsResults testUnitResults = new TestUnitsResults(this);

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

                testUnitResults.addTestUnit(currentTest, res, null);
            } catch (Throwable t) {
                Log.error("Testing " + currentTest, t);
                testUnitResults.addTestUnit(currentTest, null, t);
            }

        }

        return testUnitResults;
    }

    public boolean isRunmethod() {
        TableSyntaxNode tsn = (TableSyntaxNode) getSyntaxNode();
        return XlsNodeTypes.XLS_RUN_METHOD.toString().equals(tsn.getType());
    }
    
    /**
     * Indicates if test method has any row rules for testing target table.
     * Finds it by field that contains {@link TestMethodHelper#EXPECTED_RESULT_NAME} or 
     * {@link TestMethodHelper#EXPECTED_ERROR}
     * 
     * @return true if method expects some return result or some error.
     * 
     * TODO: rename it. it is difficult to understand what is it doing 
     */
    public boolean isRunmethodTestable() {
        // gets the data from rows that have test parameters.
        Object testArray = getBoundNode().getField().getData();

        DynamicObject[] testArrayDynamicObj = (DynamicObject[]) testArray;

        for (int i = 0; i < testArrayDynamicObj.length; i++) {
            if (testArrayDynamicObj[i].containsField(TestMethodHelper.EXPECTED_RESULT_NAME) || 
                    testArrayDynamicObj[i].containsField(TestMethodHelper.EXPECTED_ERROR) || containsFieldsForSprCellTests(testArrayDynamicObj[i].getFieldValues().keySet())) {
                return true;
            }
        }

        return false;
    }
    
    private boolean containsFieldsForSprCellTests(Set<String> fieldNames) {
        for (String fieldName : fieldNames) {
            if (fieldName.startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
                return true;
            }
        }
        return false;
    }

    public int nUnitRuns() {
        return getNumberOfTests();
    }

    public Object run(int tid, Object target, IRuntimeEnv env, int ntimes) {

        Object testArray = getBoundNode().getField().get(target, env);

        DynamicObject[] dd = (DynamicObject[]) testArray;
//        Class<? extends Object> class1 = dd[0].getFieldValue("policy").getClass();
//        try {
//            Method method = class1.getMethod("getVehicles");
//            Object[] arr = (Object[])method.invoke(dd[0].getFieldValue("policy"), new Object[]{});
//            for(Object object: arr){
//                ClassLoader classLoader = object.getClass().getClassLoader();
//                System.out.println(classLoader);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        IOpenClass dclass = getMethodBasedClass(null);
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
