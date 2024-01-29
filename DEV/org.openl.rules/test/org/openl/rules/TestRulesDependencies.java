package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.binding.BindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;
import org.openl.types.impl.ExecutableMethod;

public class TestRulesDependencies extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/TestRulesDependencies.xls";

    public TestRulesDependencies() {
        super(SRC);
    }

    @Test
    public void testDTExistingDependency() {
        String tableName = "Rules String test1(int age)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((IDecisionTable) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(1, rulesMethods.size(), "There is only one rules dependency");

            ExecutableMethod dependentMethod = (ExecutableMethod) findTable("Rules int getCalcAge(int constant)")
                .getMember();
            boolean f = false;
            for (ExecutableMethod executableMethod : rulesMethods) {
                if (executableMethod.getName().equals(dependentMethod.getName())) {
                    f = true;
                }
            }
            assertTrue(f, "DT contains expected dependency");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    public void testDTNotExistingDependency() {
        String tableName = "Rules int getCalcAge(int constant)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((IDecisionTable) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(0, rulesMethods.size(), "There is no dependencies to other rules methods");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    public void testSpreadsheet() {
        String tableName = "Spreadsheet SpreadsheetResult processDriver(Driver driver)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((Spreadsheet) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(8, rulesMethods.size(), "There is 8 rules dependencies");

            Set<ExecutableMethod> expectedRuledDependencies = new HashSet<>();
            expectedRuledDependencies
                .add((ExecutableMethod) findTable("Rules String driverAgeType(Driver driver)").getMember());
            expectedRuledDependencies
                .add((ExecutableMethod) findTable("Rules String driverEligibility(Driver driver, String ageType)")
                    .getMember());
            expectedRuledDependencies
                .add((ExecutableMethod) findTable("Rules String driverRisk(Driver driver)").getMember());
            expectedRuledDependencies.add((ExecutableMethod) findTable(
                "Rules DoubleValue driverTypeScore(String driverAgeType, String driverEligibility)").getMember());
            expectedRuledDependencies.add(
                (ExecutableMethod) findTable("Rules DoubleValue driverPremium(Driver driver, String driverAgeType)")
                    .getMember());
            expectedRuledDependencies
                .add((ExecutableMethod) findTable("Rules DoubleValue driverRiskScore(String driverRisk)").getMember());
            expectedRuledDependencies.add(
                (ExecutableMethod) findTable("Rules DoubleValue driverRiskPremium(String driverRisk)").getMember());
            expectedRuledDependencies.add((ExecutableMethod) findTable(
                "Rules DoubleValue driverAccidentPremium(Driver driver, String driverRisk)").getMember());

            int d = 0;
            for (ExecutableMethod executableMethod : rulesMethods) {
                for (ExecutableMethod expectedRuledDependency : expectedRuledDependencies) {
                    if (executableMethod.getName().equals(expectedRuledDependency.getName())) {
                        d++;
                    }
                }
            }

            assertEquals(expectedRuledDependencies.size(), d, "Spreadsheet contains all expected dependencies");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    public void testTbasic() {
        String tableName = "TBasic int factorial(int n)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((Algorithm) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(1, rulesMethods.size(), "There is only one rules dependency");

            ExecutableMethod dependentMethod = (ExecutableMethod) findTable("Method void foo()").getMember();
            boolean f = false;
            for (ExecutableMethod executableMethod : rulesMethods) {
                if (executableMethod.getName().equals(dependentMethod.getName())) {
                    f = true;
                }
            }
            assertTrue(f, "TBasic contains expected dependency");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    public void testMethod() {
        String tableName = "Method int start()";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((TableMethod) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(2, rulesMethods.size(), "There is 2 rules dependency");

            Set<ExecutableMethod> expectedRuledDependencies = new HashSet<>();
            expectedRuledDependencies.add((ExecutableMethod) findTable("Method int start2()").getMember());
            expectedRuledDependencies.add((ExecutableMethod) findTable("Method void callVoid()").getMember());

            int d = 0;
            for (ExecutableMethod executableMethod : rulesMethods) {
                for (ExecutableMethod expectedRuledDependency : expectedRuledDependencies) {
                    if (executableMethod.getName().equals(expectedRuledDependency.getName())) {
                        d++;
                    }
                }
            }

            assertEquals(expectedRuledDependencies.size(), d, "Method contains expected dependencies");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    public void tesTestTable() {
        IOpenClass moduleOpenClass = getCompiledOpenClass().getOpenClass();
        TestSuiteMethod testMethod = (TestSuiteMethod) moduleOpenClass.getMethod("riskScoreTest", IOpenClass.EMPTY);

        BindingDependencies bindDep = testMethod.getDependencies();
        Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
        assertEquals(1, rulesMethods.size(), "There is 1 rule dependency");

        Set<ExecutableMethod> expectedRuledDependencies = new HashSet<>();
        expectedRuledDependencies
            .add((ExecutableMethod) findTable("Rules DoubleValue riskScore(String driverRisk)").getMember());
        boolean allContains = true;
        for (ExecutableMethod method : expectedRuledDependencies) {
            boolean f = false;
            for (ExecutableMethod executableMethod : rulesMethods) {
                if (executableMethod.getName().equals(method.getName())) {
                    f = true;
                    break;
                }
            }
            allContains = allContains && f;
        }

        assertTrue(allContains, "Method contains expected dependency");
    }

}
