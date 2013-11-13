package org.openl.types.java;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.openl.meta.StringValue;
import org.openl.types.IOpenClass;

public class JavaOpenClassTest {
    
    @Test
    public void testGetComponentType() {
        assertEquals(JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String[].class).getComponentClass());
        
        assertEquals(JavaOpenClass.getOpenClass(String[].class), JavaOpenClass.getOpenClass(String[][].class).getComponentClass());
        
        assertNull(JavaOpenClass.getOpenClass(int.class).getComponentClass());
        
        assertEquals(JavaOpenClass.OBJECT, JavaOpenClass.getOpenClass(List.class).getComponentClass());
    }
    
    @Test
    public void testIsSimple() {
    	IOpenClass clazz = JavaOpenClass.getOpenClass(StringValue.class);
    	assertTrue(clazz.isSimple());
    }
}
