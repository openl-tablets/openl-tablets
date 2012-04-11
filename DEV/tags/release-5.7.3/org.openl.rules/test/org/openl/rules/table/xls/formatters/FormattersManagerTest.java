package org.openl.rules.table.xls.formatters;

import static org.junit.Assert.*;

import java.util.Calendar;

import org.junit.Test;
import org.openl.util.formatters.FormatterAdapter;
import org.openl.util.formatters.IFormatter;

public class FormattersManagerTest {
    
    private enum TestValues {
        FIRST_VALUE,
        SECOND_VALUE;
    }
    
    @Test
    public void testDouble() {
        Double dd = Double.valueOf(12.345);
        IFormatter formatter = FormattersManager.getFormatter(dd);
        assertTrue(formatter instanceof XlsNumberFormatter);
    }
    
    @Test
    public void testNull() {        
        IFormatter formatter = FormattersManager.getFormatter(null);
        assertTrue(formatter instanceof FormatterAdapter);
    }
    
    @Test
    public void testString() {        
        IFormatter formatter = FormattersManager.getFormatter("text");
        assertTrue(formatter instanceof FormatterAdapter);
    }
    
    @Test
    public void testDate() {        
        Calendar date = Calendar.getInstance();
        IFormatter formatter = FormattersManager.getFormatter(date.getTime());
        assertTrue(formatter instanceof XlsDateFormatter);
    }
    
    @Test
    public void testBoolean() {
        IFormatter formatter = FormattersManager.getFormatter(Boolean.TRUE);
        assertTrue(formatter instanceof XlsBooleanFormatter);
    }
    
    @Test
    public void testArray() {
        Integer[] intArray = new Integer[]{12, 34};
        IFormatter formatter = FormattersManager.getFormatter(intArray);
        assertTrue(formatter instanceof XlsArrayFormatter);
    }
    
    @Test
    public void testEnums() {        
        IFormatter formatter = FormattersManager.getFormatter(TestValues.FIRST_VALUE);
        assertTrue(formatter instanceof XlsEnumFormatter);
    }
}
