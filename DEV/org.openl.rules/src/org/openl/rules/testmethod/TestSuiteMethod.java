package org.openl.rules.testmethod;

import java.util.Set;

import org.openl.binding.BindingDependencies;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.IBenchmarkableMethod;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends ExecutableRulesMethod implements IBenchmarkableMethod {

    private IOpenMethod testedMethod;
    private TestDescription[] tests;

    public TestSuiteMethod(IOpenMethod testedMethod, IOpenMethodHeader header,
            TestMethodBoundNode boundNode) {
        super(header, boundNode);

        this.testedMethod = testedMethod;
        initProperties(getSyntaxNode().getTableProperties());
    }

    protected TestDescription[] initTests() {
        DynamicObject[] testObjects = getTestObejcts();
        TestDescription[] tests = new TestDescription[testObjects.length];
        for (int i = 0; i < tests.length; i++) {
            tests[i] = new TestDescription(getTestedMethod(), testObjects[i], this.getProperties());
            tests[i].setIndex(i);
        }
        return tests;
    }

    @Override
    public TestMethodBoundNode getBoundNode() {
        return (TestMethodBoundNode) super.getBoundNode();
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
        return getTests().length;
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public DynamicObject[] getTestObejcts() {
        Object testArray = getBoundNode().getField().getData();
        return (DynamicObject[]) testArray;
    }

    public TestDescription[] getTests() {
        if (tests == null) {
            this.tests = initTests();
        }
        return tests;
    }

    public TestDescription getTest(int numberOfTest) {
        return getTests()[numberOfTest];
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        int columnIndex = getBoundNode().getTable().getColumnIndex(columnTechnicalName);
        return getColumnDisplayName(columnIndex);
    }

    public String getColumnName(int index) {
        if (index >= 0) {
            return getBoundNode().getTable().getColumnName(index);
        } else {
            return null;
        }
    }

    public String getColumnDisplayName(int index) {
        if (index >= 0) {
            return getBoundNode().getTable().getColumnDisplay(index);
        } else {
            return null;
        }
    }

    public int getColumnsCount() {
        return getBoundNode().getTable().getNumberOfColumns();
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    @Override
    protected boolean isMethodCacheable() {
        return false;
    }

    protected TestUnitsResults innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return invokeBenchmark(target, params, env, 1);
    }

    public TestUnitsResults invokeBenchmark(Object target, Object[] params, IRuntimeEnv env, long ntimes) {
        return new TestSuite(this).invoke(target, env, ntimes);
    }

    public boolean isRunmethod() {
        TableSyntaxNode tsn = (TableSyntaxNode) getSyntaxNode();
        return XlsNodeTypes.XLS_RUN_METHOD.toString().equals(tsn.getType());
    }

    /**
     * Indicates if test method has any row rules for testing target table.
     * Finds it by field that contains
     * {@link TestMethodHelper#EXPECTED_RESULT_NAME} or
     * {@link TestMethodHelper#EXPECTED_ERROR}
     * 
     * @return true if method expects some return result or some error.
     * 
     *         TODO: rename it. it is difficult to understand what is it doing
     */
    public boolean isRunmethodTestable() {
        for (int i = 0; i < getNumberOfTests(); i++) {
            if (getTest(i).isExpectedResultDefined() || getTest(i).isExpectedErrorDefined()
                    || containsFieldsForSprCellTests(getTest(i).getTestObject().getFieldValues().keySet())
                    || (testedMethod instanceof Spreadsheet)) {
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
}
