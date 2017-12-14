package org.openl.rules.testmethod;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.BindingDependencies;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DynamicObject;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends ExecutableRulesMethod {

    private final static String PRECISION_PARAM = "precision";
    private IOpenMethod testedMethod;
    private TestDescription[] tests;
    private Map<String, Integer> indeces;
    private final boolean runmethod;
    private DynamicObject[] testObjects;
    private ColumnDescriptor[] descriptors;

    public TestSuiteMethod(IOpenMethod testedMethod, IOpenMethodHeader header,
            TestMethodBoundNode boundNode) {
        super(header, boundNode);

        this.testedMethod = testedMethod;
        initProperties(getSyntaxNode().getTableProperties());
        runmethod = XlsNodeTypes.XLS_RUN_METHOD.toString().equals(getSyntaxNode().getType());
    }

    public TestSuiteMethod(IOpenMethod testedMethod, TestSuiteMethod copy) {
        super(copy.getHeader(), copy.getBoundNode());

        this.testedMethod = testedMethod;
        initProperties(copy.getMethodProperties());
        this.runmethod = copy.isRunmethod();
        this.testObjects = copy.getTestObjects();
        this.descriptors = copy.getDescriptors();
        this.setTableUri(copy.getTableUri());
    }

    private TestDescription[] initTestsAndIndexes() {
        DynamicObject[] testObjects = getTestObjects();
        TestDescription[] tests = new TestDescription[testObjects.length];
        indeces = new HashMap<String, Integer>(tests.length);
        Map<String, Object> properties = getProperties();
        Integer precision = null;
        if (properties != null && properties.containsKey(PRECISION_PARAM)) {
            precision = Integer.parseInt(properties.get(PRECISION_PARAM).toString());
        }

        for (int i = 0; i < tests.length; i++) {
            tests[i] = new TestDescription(getTestedMethod(), testObjects[i], precision, getDescriptors());
            tests[i].setIndex(i);
            indeces.put(tests[i].getId(), i);
        }
        return tests;
    }

    public synchronized int[] getIndices(String ids) {
        if (tests == null){
            initTestsAndIndexes();
        }
        TreeSet<Integer> result = new TreeSet<Integer>();

        String ranges[] = ids.trim().split(",");
        for(String range: ranges) {
            if (range.isEmpty() && indeces.containsKey(",")) {
                result.add(indeces.get(","));
                continue;
            }
            String v = range.trim();
            if (indeces.containsKey(v)) {
                result.add(indeces.get(v));
                continue;
            }
            String edges[] = v.split("-");
            if (edges.length > 2 || edges[edges.length - 1].trim().isEmpty()) {
                edges = v.split("\\s[-]\\s");
            }
            if (edges.length == 0) {
                if (indeces.containsKey("-")) {
                    result.add(indeces.get("-"));
                } 
            } else {
                String startIdValue = edges[0].trim();
                String endIdValue = edges[edges.length - 1].trim();
    
                int startIndex = indeces.get(startIdValue);
                int endIndex = indeces.get(endIdValue);
    
                for (int i = startIndex; i<=endIndex; i++) {
                    result.add(i);
                }
            }
        }
        Integer[] indices = new Integer[result.size()];
        return ArrayUtils.toPrimitive(result.toArray(indices));
    }

    @Override
    public TestMethodBoundNode getBoundNode() {
        return (TestMethodBoundNode) super.getBoundNode();
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

    public DynamicObject[] getTestObjects() {
        initializeTestData();
        return testObjects;
    }

    public synchronized TestDescription[] getTests() {
        if (tests == null) {
            this.tests = initTestsAndIndexes();
        }
        return tests;
    }

    public TestDescription getTest(int numberOfTest) {
        return getTests()[numberOfTest];
    }
    
    public void setTestedMethod(IOpenMethod testedMethod) {
        this.testedMethod = testedMethod;
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        int columnIndex = getColumnIndex(columnTechnicalName);
        return getColumnDisplayName(columnIndex);
    }

    public int getColumnIndex(String columnName) {
        ColumnDescriptor[] descriptors = getDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i] == null) {
                continue;
            }
            if (descriptors[i].getName().equals(columnName)) {
                return i;
            }
        }

        return -1;
    }


    public String getColumnName(int index) {
        if (index >= 0) {
            ColumnDescriptor[] descriptors = getDescriptors();
            return descriptors[index] == null ? null : descriptors[index].getName();
        } else {
            return null;
        }
    }

    public String getColumnDisplayName(int index) {
        if (index >= 0) {
            ColumnDescriptor[] descriptors = getDescriptors();
            return descriptors[index] == null ? null : descriptors[index].getDisplayName();
        } else {
            return null;
        }
    }

    public int getColumnsCount() {
        return getDescriptors().length;
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    @Override
    protected boolean isMethodCacheable() {
        return false;
    }

    protected TestUnitsResults innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return new TestSuite(this).invoke(target, env);
    }

    public boolean isRunmethod() {
        return runmethod;
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

    @Override
    public void setBoundNode(ATableBoundNode node) {
        if (node == null) {
            // removeDebugInformation() is invoked.
            // Initialize data needed to run tests before removing debug info
            initializeTestData();
        }

        super.setBoundNode(node);
    }

    public ColumnDescriptor[] getDescriptors() {
        initializeTestData();
        return descriptors;
    }

    private void initializeTestData() {
        if (descriptors == null) {
            testObjects = (DynamicObject[]) getBoundNode().getField().getData();
            descriptors = getBoundNode().getTable().getDataModel().getDescriptor();
        }
    }
}
