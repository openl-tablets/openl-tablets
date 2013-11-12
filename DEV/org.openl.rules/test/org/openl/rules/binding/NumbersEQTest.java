package org.openl.rules.binding;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * Testing following constructions working in rules:
 *  DoubleValue a = new DoubleValue(-5);
 *  DoubleValue b = new DoubleValue(-5);
 *  a==b; - will be true
 * 
 * @author DLiauchuk
 *
 */
public class NumbersEQTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/binding/NumbersEQTest.xls";
    
    public NumbersEQTest() {
        super(SRC);
    }
    
    @Test
    public void testDoubleValueEQ() {
        Boolean result = (Boolean)invokeMethod("testDVEquals");
        assertTrue(result);
    }
    
    @Test
    public void testDoubleEQ() {
        Boolean result = (Boolean)invokeMethod("testDDEquals");
        assertTrue(result);
    }
}
