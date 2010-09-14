package org.openl.rules.table.xls.formatters;

import static org.junit.Assert.*;

import org.junit.Test;

public class XlsFormattersTest {
    
    @Test
    public void testPrimitiveArray() {
        int[] intMas = new int[]{1, 2};
        IFormatter formatter = XlsFormattersManager.getFormatter(intMas.getClass());
        assertEquals("1,2", formatter.format(intMas));
    }
    
    @Test
    public void testMultiDimPrimitiveArray() {
        double[][] doubleMas = new double[][]{new double[]{1.27, 5.8987}, new double[]{45.345, 123.4578}};
        IFormatter formatter = XlsFormattersManager.getFormatter(doubleMas.getClass());
        assertEquals("1.27,5.8987,45.345,123.4578", formatter.format(doubleMas));
    }
    
    @Test
    public void testBooleanParse() {
        String boolValue = "yes";
        IFormatter formatter = XlsFormattersManager.getFormatter(Boolean.class);
        assertEquals(true, formatter.parse(boolValue));
        
        boolValue = "NO";
        assertEquals(false, formatter.parse(boolValue));
        
        boolValue = "Y";
        assertEquals(true, formatter.parse(boolValue));
        
        boolValue = "N";
        assertEquals(false, formatter.parse(boolValue));
        
        boolValue = "true";
        assertEquals(true, formatter.parse(boolValue));
        
        boolValue = "false";
        assertEquals(false, formatter.parse(boolValue));
        
        boolValue = "on";
        assertEquals(true, formatter.parse(boolValue));
        
        boolValue = "off";
        assertEquals(false, formatter.parse(boolValue));        
    }
    
    @Test
    public void testBooleanFormat() {
        Boolean boolValue = Boolean.TRUE;
        IFormatter formatter = XlsFormattersManager.getFormatter(Boolean.class);
        assertEquals("true", formatter.format(boolValue));
        
        boolValue = Boolean.FALSE;
        assertEquals("false", formatter.format(boolValue));
    }

}
