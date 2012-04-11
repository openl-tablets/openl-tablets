package org.openl.binding.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.binding.IBoundNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class VariableLengthArgumentsMethodBinderTest {
    
    private static IOpenClass[] argumentsTestCase1 = getTestArguments(JavaOpenClass.getOpenClass(String.class),
        JavaOpenClass.getOpenClass(Integer.class),
        JavaOpenClass.getOpenClass(Integer.class));
    
    private static IOpenClass[] argumentsTestCase2 = getTestArguments(JavaOpenClass.getOpenClass(String.class), 
        JavaOpenClass.getOpenClass(Integer.class));
    
    private static IOpenClass[] argumentsTestCase3 = getTestArguments(JavaOpenClass.getOpenClass(String.class));
    
    private static IOpenClass[] argumentsTestCase4 = getTestArguments(JavaOpenClass.getOpenClass(String.class),
        JavaOpenClass.getOpenClass(String.class),
        JavaOpenClass.getOpenClass(String.class));
    
    private static IOpenClass[] getTestArguments(IOpenClass... classes) {
        return classes;
    }
    
    @Test
    public void test1() {
        VariableLengthArgumentsMethodBinder binder = 
            new VariableLengthArgumentsMethodBinder("test", argumentsTestCase1, new IBoundNode[0]);
        assertEquals(1, binder.getIndexOfFirstVarArg());
        
        VariableLengthArgumentsMethodBinder binder1 = new VariableLengthArgumentsMethodBinder("test", argumentsTestCase2, 
            new IBoundNode[0]);
        assertEquals(1, binder1.getIndexOfFirstVarArg());
        
        VariableLengthArgumentsMethodBinder binder2 = new VariableLengthArgumentsMethodBinder("test", argumentsTestCase3,
            new IBoundNode[0]);
        assertEquals(0, binder2.getIndexOfFirstVarArg());
        
        VariableLengthArgumentsMethodBinder binder3 = 
            new VariableLengthArgumentsMethodBinder("test", argumentsTestCase4, new IBoundNode[0]);
        assertEquals(0, binder3.getIndexOfFirstVarArg());
    }
    
    
}
