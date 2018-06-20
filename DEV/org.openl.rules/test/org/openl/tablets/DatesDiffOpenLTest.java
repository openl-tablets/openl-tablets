package org.openl.tablets;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.util.Dates;
import org.openl.util.DateTool;

public class DatesDiffOpenLTest {

    private static final String SRC = "test/rules/DateDifference.xls";
    private IDateDifferenceTest instance;

    @Before
    public void initEngine() {
        RulesEngineFactory<IDateDifferenceTest> engineFactory = new RulesEngineFactory<>(SRC,
            IDateDifferenceTest.class);

        instance = engineFactory.newEngineInstance();
    }

    // ------------Testing via Openl-------------------
    @Test
    public void testViaRule1() throws Exception {
        Date startDate = getDate("01/01/1969");

        Date endDate = getDate("02/08/2010");

        int diff = instance.dateCount(startDate, endDate);
        assertEquals(15188, diff);
    }

    @Test
    public void testViaRule2() throws Exception {
        Date startDate = getDate("01/01/1960");

        Date endDate = getDate("02/08/2010");

        int diff = instance.dateCount(startDate, endDate);
        assertEquals(18476, diff);
    }

    @Test
    public void testViaRule3() throws Exception {
        Date startDate = getDate("01/01/1970");

        Date endDate = getDate("02/08/2010");

        int diff = instance.dateCount(startDate, endDate);
        assertEquals(14823, diff);
    }

    @Test
    public void testMonthDiff() throws Exception {
        Date startDate = getDate("01/01/1970");

        Date endDate = getDate("02/08/2010");

        Integer oldRes = DateTool.monthDiff(endDate, startDate);

        Integer newRes = Dates.dateDif(startDate, endDate, "M").intValue();

        assertEquals(oldRes, newRes);
    }

    // ------------End Testing via Openl-------------------

    private Date getDate(String stringDate) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        return dateFormat.parse(stringDate);
    }
}
