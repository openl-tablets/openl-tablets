/**
 * OpenL Tablets,  2009
 * https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.tablets.tutorial7;

import static java.lang.System.out;

import org.openl.rules.testmethod.TestResult;

/**
 * Tutorial 7. Example of ColumnMatch tables.
 * <p>
 * Run this class "as Java Application".
 * 
 * @author Aleh Bykhavets
 */

public class Tutorial7Main {
    public static void main(String[] args) {
        out.println();
        out.println("* OpenL Tutorial 7\n");

        out.println("Getting wrapper...");
        Tutorial_7Wrapper tutorial7 = new Tutorial_7Wrapper();

        out.println("* Executing OpenL tables...\n");
        Expense expense = new Expense();
        expense.setArea("Hardware");
        expense.setMoney(55000.0);

        out.println("needApproval(Expense):");
        out.printf("  %s: %s\n", expense, tutorial7.needApprovalOf(expense));
        expense.setMoney(5000.0);
        out.printf("  %s: %s\n", expense, tutorial7.needApprovalOf(expense));
        out.println();

        Issue issue1 = issue("Other", true, 5400.0);
        Issue issue2 = issue("Profit", true, 15000.0);
        Issue issue3 = issue("Loss", false, 1500.0);

        out.println("scoreIssue(Issue):");
        out.printf("  %s: %d\n", issue1, tutorial7.scoreIssue(issue1));
        out.printf("  %s: %d\n", issue2, tutorial7.scoreIssue(issue2));
        out.printf("  %s: %d\n", issue3, tutorial7.scoreIssue(issue3));
        out.println();

        out.println("scoreIssueImportance(Issue):");
        out.printf("  %s: %s\n", issue1, tutorial7.scoreIssueImportance(issue1));
        out.printf("  %s: %s\n", issue2, tutorial7.scoreIssueImportance(issue2));
        out.printf("  %s: %s\n", issue3, tutorial7.scoreIssueImportance(issue3));
        out.println();

        out.println("* Executing TestMethod tables...\n");
        report(tutorial7.test1TestAll());
        report(tutorial7.test2TestAll());
        report(tutorial7.test3TestAll());
    }

    private static void report(TestResult testResult) {
        out.println(testResult.getName());

        int fails = testResult.getNumberOfFailures();
        int total = testResult.getNumberOfTests();
        if (fails == 0) {
            out.println("  All GREEN");
        } else {
            out.printf("  %d test(s) of %d FAILED!\n", fails, total);
        }

        for (int i = 0; i < total; i++) {
            out.printf("  %d: ", i);
            if (testResult.getCompareResult(i) == TestResult.TR_OK) {
                out.printf("OK, %s\n", testResult.getResult(i));
            } else {
                out.printf("FAILED! Expect <%s> but get <%s>!\n", testResult.getExpected(i), testResult.getResult(i));
            }
        }
        out.println();
    }

    private static Issue issue(String area, boolean isMundane, double money) {
        Issue i = new Issue();

        i.setArea(area);
        i.setMundane(isMundane);
        i.setMoney(money);

        return i;
    }
}
