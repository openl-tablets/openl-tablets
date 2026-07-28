package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IOpenClass;
import org.openl.types.impl.ExecutableMethod;

class TestRulesDependencies extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/TestRulesDependencies.xls";

    public TestRulesDependencies() {
        super(SRC);
    }

    @Test
    void testDTExistingDependency() {
        var tableName = "Rules String test1(int age)";
        var tsn = findTable(tableName);
        if (tsn != null) {
            var bindDep = ((IDecisionTable) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(1, rulesMethods.size(), "There is only one rules dependency");

            var dependentMethod = (ExecutableMethod) findTable("Rules int getCalcAge(int constant)")
                    .getMember();
            var f = false;
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
    void testDTNotExistingDependency() {
        var tableName = "Rules int getCalcAge(int constant)";
        var tsn = findTable(tableName);
        if (tsn != null) {
            var bindDep = ((IDecisionTable) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(0, rulesMethods.size(), "There is no dependencies to other rules methods");
        } else {
            fail("Cannot find expected table");
        }
    }

    @Test
    void testSpreadsheet() {
        var tableName = "Spreadsheet SpreadsheetResult processDriver(Driver driver)";
        var tsn = findTable(tableName);
        if (tsn != null) {
            var bindDep = ((Spreadsheet) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(8, rulesMethods.size(), "There is 8 rules dependencies");

            var expectedRuledDependencies = new HashSet<ExecutableMethod>();
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

            var d = 0;
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
    void testTbasic() {
        var tableName = "TBasic int factorial(int n)";
        var tsn = findTable(tableName);
        if (tsn != null) {
            var bindDep = ((Algorithm) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(1, rulesMethods.size(), "There is only one rules dependency");

            var dependentMethod = (ExecutableMethod) findTable("Method void foo()").getMember();
            var f = false;
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
    void testMethod() {
        var tableName = "Method int start()";
        var tsn = findTable(tableName);
        if (tsn != null) {
            var bindDep = ((TableMethod) tsn.getMember()).getDependencies();
            Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals(2, rulesMethods.size(), "There is 2 rules dependency");

            var expectedRuledDependencies = new HashSet<ExecutableMethod>();
            expectedRuledDependencies.add((ExecutableMethod) findTable("Method int start2()").getMember());
            expectedRuledDependencies.add((ExecutableMethod) findTable("Method void callVoid()").getMember());

            var d = 0;
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
    void tesTestTable() {
        var moduleOpenClass = getCompiledOpenClass().getOpenClass();
        var testMethod = (TestSuiteMethod) moduleOpenClass.getMethod("riskScoreTest", IOpenClass.EMPTY);

        var bindDep = testMethod.getDependencies();
        Set<ExecutableMethod> rulesMethods = bindDep.getRulesMethods();
        assertEquals(1, rulesMethods.size(), "There is 1 rule dependency");

        var expectedRuledDependencies = new HashSet<ExecutableMethod>();
        expectedRuledDependencies
                .add((ExecutableMethod) findTable("Rules DoubleValue riskScore(String driverRisk)").getMember());
        var allContains = true;
        for (ExecutableMethod method : expectedRuledDependencies) {
            var f = false;
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
