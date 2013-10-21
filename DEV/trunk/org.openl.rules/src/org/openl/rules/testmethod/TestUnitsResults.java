/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.ITableModel;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.testmethod.TestUnitResultComparator.TestStatus;
import org.openl.rules.testmethod.result.TestResultComparator;
import org.openl.rules.testmethod.result.TestResultComparatorFactory;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * Test units results for the test table.
 * Consist of the test suit method itself. And a number of test units that were represented in test table.
 * 
 *
 */
public class TestUnitsResults implements INamedThing {
    
    private TestSuite testSuite;
    private ArrayList<TestUnit> testUnits = new ArrayList<TestUnit>();

    public TestUnitsResults(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public String getName() {
        return testSuite.getDisplayName(INamedThing.SHORT);
    }

    public String getDisplayName(int mode) {
        return testSuite.getDisplayName(mode);
    }

    public ArrayList<TestUnit> getTestUnits() {
        return testUnits;
    }

    public void addTestUnit(TestUnit testUnit) {
        if (!testSuite.isVirtualTestSuite()) {
            testUnits.add(updateTestUnit(testUnit));
        } else {
            testUnits.add(testUnit);
        }
    }
    
    /**
     * Creates the list of test unit results. 
     * 
     * @param testedMethod method that is tested by test
     * @param testObj instance of the object that was used as input test data
     * @param runningResult result of running the test
     * @param ex exception during test running
     * @return list of test unit results
     * 
     * FIXME it should be moved to compile phase and all info about bean comparator should be located in {@link TestDescription}
     */
    public TestUnit updateTestUnit(TestUnit testUnit) {
        ITableModel dataModel = testSuite.getTestSuiteMethod().getBoundNode().getTable().getDataModel();
        List<IOpenField> fieldsToTest = new ArrayList<IOpenField>();

        IOpenClass resultType = testSuite.getTestedMethod().getType();
        Integer precision = null;
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptor()) {

            if (columnDescriptor != null) {
                IdentifierNode[] nodes = columnDescriptor.getFieldChainTokens();
                if (nodes.length > 1 && TestMethodHelper.EXPECTED_RESULT_NAME.equals(nodes[0].getIdentifier())) {
                    if (columnDescriptor.isReference()) {
                        if (!resultType.isSimple()) {
                            fieldsToTest.addAll(resultType.getFields().values());
                        }
                    } else {
                        // get the field name next to _res_ field, e.g.
                        // "_res_.$Value$Name"
                        if (nodes.length > 2) {
                            IOpenField[] fieldSequence = new IOpenField[nodes.length - 1];
                            IOpenClass currentType = resultType;
                            for (int i = 0; i < fieldSequence.length; i++) {
                                fieldSequence[i] = currentType.getField(nodes[i + 1].getIdentifier());

                                if (fieldSequence[i] == null) {
                                    if (nodes[i + 1].getIdentifier().matches(DataTableBindHelper.PRECISION_PATTERN)) {
                                        precision = DataTableBindHelper.getPrecisionValue(nodes[i + 1]);
                                        fieldSequence = (IOpenField[]) ArrayUtils.remove(fieldSequence, i);

                                        break;
                                    }
                                }

                                currentType = fieldSequence[i].getType();
                            }

                            if (precision != null) {
                                fieldsToTest.add(new PrecisionFieldChain(currentType, fieldSequence, precision));
                            } else {
                                fieldsToTest.add(new FieldChain(currentType, fieldSequence));
                            }
                        } else {
                            if (nodes[1].getIdentifier().matches(DataTableBindHelper.PRECISION_PATTERN)) {
                                precision = DataTableBindHelper.getPrecisionValue(nodes[1]);
                                continue;
                            } else {
                                fieldsToTest.add(resultType.getField(nodes[1].getIdentifier()));
                            }
                        }
                    }
                }
            }
        }

        if (fieldsToTest.size() > 0) {
            TestResultComparator resultComparator = TestResultComparatorFactory.getOpenLBeanComparator(fieldsToTest);
            testUnit.setTestUnitResultComparator(new TestUnitResultComparator(resultComparator));
        } else if (precision != null){
            testUnit.setPrecision(precision);
        }
        return testUnit;
    }

    public void addTestUnits(List<TestUnit> testUnits) {
        testUnits.addAll(testUnits);
    }

    @Deprecated
    public Object getExpected(int i) {
        return testUnits.get(i).getExpectedResult();
    }

    public int getNumberOfFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestStatus.TR_OK.getStatus()) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfTestUnits() {
        return testUnits.size();
    }

    public boolean isAnyUnitHasDescription() {
        for (TestUnit testUnit: testUnits) {
            if (!testUnit.getDescription().equals(TestUnit.DEFAULT_DESCRIPTION)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpreadsheetResultTester() {
        return ClassUtils.isAssignable(testSuite.getTestedMethod().getType().getInstanceClass(), SpreadsheetResult.class, false);
    }

    public boolean isRunmethod() {
        return testSuite.getTestSuiteMethod().isRunmethod();
    }

    @Deprecated
    public Object getUnitResult(int i) {
        return testUnits.get(i).getActualResult();
    }

    @Deprecated
    public Object getUnitDescription(int i) {
        return testUnits.get(i).getDescription();
    }

    public String[] getTestDataColumnDisplayNames() {
        String[] columnTechnicalNames = getTestDataColumnHeaders();
        String[] columnDisplayNames = new String[columnTechnicalNames.length];
        for (int i = 0; i < columnDisplayNames.length; i++) {
            columnDisplayNames[i] = testSuite.getTestSuiteMethod().getColumnDisplayName(columnTechnicalNames[i]);
        }
        return columnDisplayNames;
    }

    public String[] getTestResultColumnDisplayNames() {
        List<String> displayNames = new ArrayList<String>();
        TestSuiteMethod test = testSuite.getTestSuiteMethod();
        for (int i = 0; i < test.getColumnsCount(); i++) {
            String columnName = test.getColumnName(i);
            if (columnName.startsWith(TestMethodHelper.EXPECTED_RESULT_NAME)) {
                displayNames.add(test.getColumnDisplayName(columnName));
            }
        }
        return displayNames.toArray(new String[displayNames.size()]);
    }

    public String[] getTestDataColumnHeaders() {
        IMethodSignature testMethodSignature = testSuite.getTestedMethod().getSignature();

        int len = testMethodSignature.getParameterTypes().length;

        String[] res = new String[len];
        for (int i = 0; i < len; i++) {
            res[i] = testMethodSignature.getParameterName(i);
        }
        return res;
    }

    @Deprecated
    public Object getTestValue(String fname, int i) {

        TestUnit testUnit = testUnits.get(i);

        return testUnit.getFieldValue(fname);
    }

    public StringBuilder printFailedUnits(StringBuilder sb) {
        sb.append(getName());
        if (getNumberOfFailures() == 0) {
            return sb.append(" - ").append(getNumberOfTestUnits()).append(" tests ALL OK!");
        }

        sb.append(" - ").append(getNumberOfTestUnits()).append(" tests / ").append(getNumberOfFailures())
                .append(" FAILED!");

        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestStatus.TR_OK.getStatus()) {
                sb.append('\n').append(i + 1).append(". ").append(testUnits.get(i).getDescription()).append("\t")
                        .append(testUnits.get(i).getExpectedResult()).append(" / ")
                        .append(testUnits.get(i).getActualResult());
            }
        }

        return sb;
    }

    @Override
    public String toString() {
        return printFailedUnits(new StringBuilder()).toString();
    }
}