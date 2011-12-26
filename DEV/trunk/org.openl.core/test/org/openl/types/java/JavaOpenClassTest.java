package org.openl.types.java;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class JavaOpenClassTest {
    
    @Test
    public void testGetComponentType() {
        assertEquals(JavaOpenClass.getOpenClass(String.class), JavaOpenClass.getOpenClass(String[].class).getComponentClass());
        
        assertEquals(JavaOpenClass.getOpenClass(String[].class), JavaOpenClass.getOpenClass(String[][].class).getComponentClass());
        
        assertNull(JavaOpenClass.getOpenClass(int.class).getComponentClass());
        
        assertEquals(JavaOpenClass.OBJECT, JavaOpenClass.getOpenClass(List.class).getComponentClass());
    }
}
