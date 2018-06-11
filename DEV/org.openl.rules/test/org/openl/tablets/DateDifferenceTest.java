package org.openl.tablets;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.util.DateDifference;
import org.openl.util.DateTool;

public class DateDifferenceTest {
    
    private static final String dateFormatWithHours = "dd/MM/yyyy HH:mm";

    private static final String dateFormat = "dd/MM/yyyy";

    private static final String SRC = "test/rules/DateDifference.xls";
    
    private IDateDifferenceTest instance;
    
    @Before
    public void initEngine() {
        RulesEngineFactory<IDateDifferenceTest> engineFactory = new RulesEngineFactory<IDateDifferenceTest>(
                 SRC, IDateDifferenceTest.class);
        
        instance = engineFactory.newEngineInstance();
    }
    
    //------------Testing via Openl-------------------
    @Test
    public void testViaRule1() {        
        Date startDate = getDate(dateFormat, "01/01/1969");
        
        Date endDate = getDate(dateFormat, "02/08/2010");
        
        int diff = instance.dateCount(startDate, endDate);
        Assert.assertEquals(15188, diff);    
    }
    
    @Test
    public void testViaRule2() {        
        Date startDate = getDate(dateFormat, "01/01/1960");
        
        Date endDate = getDate(dateFormat, "02/08/2010");
        
        int diff = instance.dateCount(startDate, endDate);
        Assert.assertEquals(18476, diff);    
    }
    
    @Test
    public void testViaRule3() {        
        Date startDate = getDate(dateFormat, "01/01/1970");
        
        Date endDate = getDate(dateFormat, "02/08/2010");
        
        int diff = instance.dateCount(startDate, endDate);
        Assert.assertEquals(14823, diff);    
    }
    
    @Test
    public void testMonthDiff() {
        Date startDate = getDate(dateFormat, "01/01/1970");
        
        Date endDate = getDate(dateFormat, "02/08/2010");
        
        Integer oldRes = DateTool.monthDiff(endDate, startDate);

        Integer newRes = DateDifference.getDifferenceInMonths(endDate, startDate);
        
        assertEquals(oldRes, newRes);
    }
    
    //------------End Testing via Openl-------------------
    
    
    //------------Direct testing-------------------
    
    @Test
    public void testYearDifference1() {
        Date startDate = getDate(dateFormat, "01/01/1970");
        
        Date endDate = getDate(dateFormat, "02/08/2010");
        
        assertEquals(new Integer(40), DateDifference.getDifferenceInYears(endDate, startDate));
    }
    
    @Test
    public void testYearDifference2() {
        Date startDate = getDate(dateFormat, "01/02/1971");
        
        Date endDate = getDate(dateFormat, "01/01/1972");
        
        assertEquals(new Integer(0), DateDifference.getDifferenceInYears(endDate, startDate));
    }
    
    @Test
    public void testYearDifference3() {
        Date startDate = getDate(dateFormat, "01/02/1907");
        
        Date endDate = getDate(dateFormat, "01/01/1903");
        
        assertEquals(new Integer(-4), DateDifference.getDifferenceInYears(endDate, startDate));
    }
    
    @Test
    public void testDayDifference() {
        Date startDate = getDate(dateFormat, "31/12/1971");
        
        Date endDate = getDate(dateFormat, "01/01/1972");
        
        assertEquals(new Integer(1), DateDifference.getDifferenceInDays(endDate, startDate));
    }
    
    @Test
    public void testDayDifference1() {
        Date startDate = getDate(dateFormatWithHours, "31/12/1971 18:00");
        
        Date endDate = getDate(dateFormatWithHours, "01/01/1972 00:00");
        
        assertEquals(new Integer(0), DateDifference.getDifferenceInDays(endDate, startDate));
    }
    
    @Test
    public void testDayDifference2() {
        Date startDate = getDate(dateFormatWithHours, "31/12/1971 11:00");
        
        Date endDate = getDate(dateFormatWithHours, "01/01/1972 00:00");
        
        assertEquals(new Integer(0), DateDifference.getDifferenceInDays(endDate, startDate));
    }
    
    @Test
    public void testMonthDifference() {
        Date startDate = getDate(dateFormat, "01/01/1970");
        
        Date endDate = getDate(dateFormat, "25/08/1970");
        
        assertEquals(new Integer(7), DateDifference.getDifferenceInMonths(endDate, startDate));
    }
    
    @Test
    public void testWeekDifference() {
        Date startDate = getDate(dateFormat, "01/01/1970");
        
        Date endDate = getDate(dateFormat, "25/08/1970");
        
        assertEquals(new Integer(7), DateDifference.getDifferenceInMonths(endDate, startDate));
    }
    
    private Date getDate(String format, String stringDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);        
        Date date = null;
        try {
            date = dateFormat.parse(stringDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
        return date;
    }
    
}
