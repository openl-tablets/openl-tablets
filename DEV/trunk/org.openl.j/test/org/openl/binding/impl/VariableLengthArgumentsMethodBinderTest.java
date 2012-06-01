package org.openl.binding.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.binding.impl.VariableLengthArgumentsMethodBinder.EqualTypesVarArgsBuilder;
import org.openl.binding.impl.VariableLengthArgumentsMethodBinder.VarArgsInfo;
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
        VarArgsInfo binder = new EqualTypesVarArgsBuilder(argumentsTestCase1).build();
        assertEquals(1, binder.getFirstVarArgIndex());
        
        VarArgsInfo binder1 = new EqualTypesVarArgsBuilder(argumentsTestCase2).build();
        assertEquals(1, binder1.getFirstVarArgIndex());
        
        VarArgsInfo binder2 =  new EqualTypesVarArgsBuilder(argumentsTestCase3).build();
        assertEquals(0, binder2.getFirstVarArgIndex());
        
        VarArgsInfo binder3 =  new EqualTypesVarArgsBuilder(argumentsTestCase4).build();
        assertEquals(0, binder3.getFirstVarArgIndex());
    }
    
    
}
