package org.openl.rules.calc.result.gen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.bean.writers.MethodWriter;

public class DecoratorMethodWriterTest {
    
    @Test
    public void test() {
        assertEquals("org/openl/meta/DoubleValue", DecoratorMethodWriter.getTypeNameForCast(new FieldDescription(DoubleValue.class)));
        assertEquals("[Lorg/openl/meta/DoubleValue;", DecoratorMethodWriter.getTypeNameForCast(new FieldDescription(DoubleValue[].class)));
        assertEquals("java/lang/Integer", DecoratorMethodWriter.getTypeNameForCast(new FieldDescription(int.class)));
        assertEquals("[I", DecoratorMethodWriter.getTypeNameForCast(new FieldDescription(int[].class)));
    }
    
    @Test
    public void testForbiddenSymbols() {
        assertFalse(MethodWriter.containRestrictedSymbols("$Hello$Vasia"));
        assertFalse(MethodWriter.containRestrictedSymbols("$_Hello$Vasia"));
        assertFalse(MethodWriter.containRestrictedSymbols("$_Hello$10Vasia"));
        
        assertTrue(MethodWriter.containRestrictedSymbols("$Hello$From Georgia"));
        assertTrue(MethodWriter.containRestrictedSymbols("$Hello$//FromGeorgia"));
    }
}
