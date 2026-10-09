package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;
import org.openl.rules.testmethod.result.ComparedResult;

/**
 * A test case that expects values of the tested Spreadsheet, while the Spreadsheet throws a user error, reports the
 * expected value of every tested cell rather than the whole expected Spreadsheet.
 */
class ExpectedResultWithUserErrorTest {

    @Test
    void reportsTheExpectedValueOfEveryTestedCell() {
        var rules = TestUtils.create("test/rules/testmethod/ExpectedResultWithUserErrorTest.xlsx", Validation.class);

        var testUnit = rules.validateTest().getTestUnits().getFirst();

        assertEquals(TestStatus.TR_NEQ, testUnit.getResultStatus());
        List<ComparedResult> compared = testUnit.getComparisonResults();
        assertEquals(List.of("$Value$Check", "$Value$Doubled"),
                compared.stream().map(ComparedResult::getFieldName).toList());
        assertEquals(List.of(234, 468), compared.stream().map(ComparedResult::getExpectedValue).toList());
        assertEquals(List.of("Too big", "Too big"), compared.stream().map(ComparedResult::getActualValue).toList());
        assertEquals(2, testUnit.getNumberOfFailedTests());
    }

    public interface Validation {
        TestUnitsResults validateTest();
    }
}
