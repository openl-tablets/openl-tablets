package org.openl.rules;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.openl.binding.BindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.dt.DecisionTable;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableRulesMethod;

public class TestRulesDependencies extends BaseOpenlBuilderHelper {

    private static String __src = "test/rules/TestRulesDependencies.xls";
    
    public TestRulesDependencies() {
        super(__src);        
    }

    @Test
    public void testDTExistingDependency() {
        String tableName = "Rules String test1(int age)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((DecisionTable)tsn.getMember()).getDependencies();
            Set<ExecutableRulesMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals("There is only one rules dependency", 1, rulesMethods.size());
            
            IOpenMethod dependentMethod = (IOpenMethod)findTable("Rules int getCalcAge(int constant)").getMember();            
            assertTrue("DT contains expected dependency", rulesMethods.contains(dependentMethod));
        } else {
            fail("Can`t find expected table");
        }
    }

    @Test
    public void testDTNotExistingDependency() {
        String tableName = "Rules int getCalcAge(int constant)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((DecisionTable)tsn.getMember()).getDependencies();
            Set<ExecutableRulesMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals("There is no dependencies to other rules methods", 0, rulesMethods.size());            
        } else {
            fail("Can`t find expected table");
        }
    }
   
    @Test
    public void testSpreadsheet() {
        String tableName = "Spreadsheet SpreadsheetResult processDriver(Driver driver)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((Spreadsheet)tsn.getMember()).getDependencies();
            Set<ExecutableRulesMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals("There is 8 rules dependencies", 8, rulesMethods.size());
            
            Set<ExecutableRulesMethod> expectedRuledDependencies = new HashSet<ExecutableRulesMethod>();
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable("Rules String driverAgeType(Driver driver)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules String driverEligibility(Driver driver, String ageType)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable("Rules String driverRisk(Driver driver)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules DoubleValue driverTypeScore(String driverAgeType, String driverEligibility)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules DoubleValue driverPremium(Driver driver, String driverAgeType)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules DoubleValue driverRiskScore(String driverRisk)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules DoubleValue driverRiskPremium(String driverRisk)").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable(
                "Rules DoubleValue driverAccidentPremium(Driver driver, String driverRisk)").getMember());   
            
            assertTrue("Spreadsheet contains all expected dependencies", rulesMethods.containsAll(expectedRuledDependencies));
        } else {
            fail("Can`t find expected table");
        }   
    }

    @Test
    public void testTbasic() {        
        String tableName = "TBasic int factorial(int n)";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((Algorithm)tsn.getMember()).getDependencies();
            Set<ExecutableRulesMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals("There is only one rules dependency", 1, rulesMethods.size());
            
            IOpenMethod dependentMethod = (IOpenMethod)findTable("Method void foo()").getMember();            
            assertTrue("TBasic contains expected dependency", rulesMethods.contains(dependentMethod));
        } else {
            fail("Can`t find expected table");
        }
    }

    @Test
    public void testMethod() {
        String tableName = "Method int start()";
        TableSyntaxNode tsn = findTable(tableName);
        if (tsn != null) {
            BindingDependencies bindDep = ((TableMethod)tsn.getMember()).getDependencies();
            Set<ExecutableRulesMethod> rulesMethods = bindDep.getRulesMethods();
            assertEquals("There is 2 rules dependency", 2, rulesMethods.size());
            
            Set<ExecutableRulesMethod> expectedRuledDependencies = new HashSet<ExecutableRulesMethod>();
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable("Method int start2()").getMember());
            expectedRuledDependencies.add((ExecutableRulesMethod)findTable("Method void callVoid()").getMember());
                        
            assertTrue("Method contains expected dependencies", rulesMethods.containsAll(expectedRuledDependencies));
        } else {
            fail("Can`t find expected table");
        }
    }
}
