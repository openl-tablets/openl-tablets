package org.openl.rules.table.formatters;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.util.formatters.IFormatter;

public class FormattersTest {

    @Test
    public void testPrimitiveArray() {
        int[] intMas = new int[]{1, 2};
        IFormatter formatter = FormattersManager.getFormatter(intMas.getClass());
        assertEquals("1,2", formatter.format(intMas));
    }

    @Test
    public void testMultiDimPrimitiveArray() {
        double[][] doubleMas = new double[][]{new double[]{1.27, 5.8987}, new double[]{45.345, 123.4578}};
        IFormatter formatter = FormattersManager.getFormatter(doubleMas.getClass());
        assertEquals("1.27,5.8987,45.345,123.4578", formatter.format(doubleMas));
    }

    @Test
    public void testBooleanParse() {
        String boolValue = "yes";
        IFormatter formatter = FormattersManager.getFormatter(Boolean.class);
        customAssertTrue(formatter.parse(boolValue));
        
        boolValue = "NO";
        customAssertFalse(formatter.parse(boolValue));
        
        boolValue = "Y";
        customAssertTrue(formatter.parse(boolValue));
        
        boolValue = "N";
        customAssertFalse(formatter.parse(boolValue));
        
        boolValue = "true";
        customAssertTrue(formatter.parse(boolValue));
        
        boolValue = "false";
        customAssertFalse(formatter.parse(boolValue));
        
        boolValue = "on";
        customAssertTrue(formatter.parse(boolValue));
        
        boolValue = "off";
        customAssertFalse(formatter.parse(boolValue));
    }

    private void customAssertTrue(Object o) {
        assertNotNull(o);
        assertTrue((Boolean) o);
    }

    private void customAssertFalse(Object o) {
        assertNotNull(o);
        assertFalse((Boolean) o);
    }
    
    @Test
    public void testBooleanFormat() {
        Boolean boolValue = Boolean.TRUE;
        IFormatter formatter = FormattersManager.getFormatter(Boolean.class);
        assertEquals("true", formatter.format(boolValue));
        
        boolValue = Boolean.FALSE;
        assertEquals("false", formatter.format(boolValue));
    }

}
