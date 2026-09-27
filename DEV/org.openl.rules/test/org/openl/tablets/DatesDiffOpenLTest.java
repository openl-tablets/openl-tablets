package org.openl.tablets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.util.Dates;
import org.openl.util.DateTool;

class DatesDiffOpenLTest {

    private static final String SRC = "test/rules/DateDifference.xls";
    private IDateDifferenceTest instance;

    @BeforeEach
    void initEngine() {
        var engineFactory = new RulesEngineFactory<IDateDifferenceTest>(SRC,
                IDateDifferenceTest.class);

        instance = engineFactory.newEngineInstance();
    }

    // ------------Testing via Openl-------------------
    @ParameterizedTest(name = "{0} to 02/08/2010 is {1} days")
    @CsvSource({"01/01/1969, 15188", "01/01/1960, 18476", "01/01/1970, 14823"})
    void testViaRule(String start, int expectedDiff) throws Exception {
        var startDate = getDate(start);

        var endDate = getDate("02/08/2010");

        var diff = instance.dateCount(startDate, endDate);
        assertEquals(expectedDiff, diff);
    }

    @Test
    void testMonthDiff() throws Exception {
        var startDate = getDate("01/01/1970");

        var endDate = getDate("02/08/2010");

        Integer oldRes = DateTool.monthDiff(endDate, startDate);

        var newRes = Dates.dateDif(startDate, endDate, "M").intValue();

        assertEquals(oldRes, newRes);
    }

    // ------------End Testing via Openl-------------------

    private Date getDate(String stringDate) throws Exception {
        var dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.parse(stringDate);
    }
}
