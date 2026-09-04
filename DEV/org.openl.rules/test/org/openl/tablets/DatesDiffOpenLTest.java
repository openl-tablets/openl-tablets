package org.openl.tablets;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    @Test
    void testViaRule1() throws Exception {
        var startDate = getDate("01/01/1969");

        var endDate = getDate("02/08/2010");

        var diff = instance.dateCount(startDate, endDate);
        assertEquals(15188, diff);
    }

    @Test
    void testViaRule2() throws Exception {
        var startDate = getDate("01/01/1960");

        var endDate = getDate("02/08/2010");

        var diff = instance.dateCount(startDate, endDate);
        assertEquals(18476, diff);
    }

    @Test
    void testViaRule3() throws Exception {
        var startDate = getDate("01/01/1970");

        var endDate = getDate("02/08/2010");

        var diff = instance.dateCount(startDate, endDate);
        assertEquals(14823, diff);
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
