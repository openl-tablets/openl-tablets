package org.openl.rules.testmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.TestUtils;

/**
 * Test tables whose precision counts significant figures, written as {@code Nsf} or taken from the expected cell with
 * {@code sf}.
 *
 * @author Yury Molchan
 */
class SignificantFiguresTest {

    private static final String FILE_NAME = "test/rules/testmethod/SignificantFiguresInTests.xlsx";

    private static ICube instance;

    @BeforeAll
    static void setUp() {
        instance = TestUtils.create(FILE_NAME, ICube.class);
    }

    @Test
    void figuresOfTheExpectedCell() {
        // 2.1545 ** 3 is 10.000909453625: 10.000 allows 0.0005, 10.00 allows 0.005
        // 0.03 ** 3 is 0.000027: 0.0001 allows 0.00005
        // 1.1 ** 3 is 1.3310000000000004: the text 1.3300 allows 0.00005
        assertEquals(List.of(TestStatus.TR_NEQ, TestStatus.TR_NEQ, TestStatus.TR_NEQ, TestStatus.TR_OK),
                statuses(instance.CubeWrittenTest()));
    }

    @Test
    void figuresOfTheExpectedCellByTheTableProperty() {
        assertEquals(List.of(TestStatus.TR_NEQ, TestStatus.TR_OK), statuses(instance.CubePropertyTest()));
    }

    @Test
    void numberOfFigures() {
        // 4 figures of 1.33 allow 0.0005
        assertEquals(List.of(TestStatus.TR_NEQ, TestStatus.TR_OK), statuses(instance.CubeFiguresTest()));
    }

    private static List<TestStatus> statuses(TestUnitsResults results) {
        return results.getTestUnits().stream().map(ITestUnit::getResultStatus).toList();
    }

    public interface ICube {

        TestUnitsResults CubeWrittenTest();

        TestUnitsResults CubePropertyTest();

        TestUnitsResults CubeFiguresTest();
    }
}
