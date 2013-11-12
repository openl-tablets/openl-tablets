package org.openl.binding.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.binding.impl.VariableLengthArgumentsMethodBinder.CastableTypesVarArgsBuilder;
import org.openl.binding.impl.VariableLengthArgumentsMethodBinder.EqualTypesVarArgsBuilder;
import org.openl.binding.impl.VariableLengthArgumentsMethodBinder.VarArgsInfo;
import org.openl.binding.impl.cast.CastFactory;
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
    
    @Test
    public void testCastable() {
        CastFactory cf = new CastFactory();
        cf.setMethodFactory(new StaticClassLibrary(JavaOpenClass.getOpenClass(org.openl.binding.impl.Operators.class)));
        
        VarArgsInfo varArgs;
        
        varArgs = createCastableVarArgs(cf, double.class, double.class);
        assertEquals(0, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(double[].class), varArgs.getModifiedMethodArguments());

        varArgs = new CastableTypesVarArgsBuilder(getTestArguments(String.class, double.class, double.class), cf).build();
        assertEquals(1, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(String.class, double[].class), varArgs.getModifiedMethodArguments());

        varArgs = new CastableTypesVarArgsBuilder(getTestArguments(String.class, double.class, int.class, double.class), cf).build();
        assertEquals(1, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(String.class, double[].class), varArgs.getModifiedMethodArguments());

        varArgs = new CastableTypesVarArgsBuilder(getTestArguments(String.class, int.class, double.class, int.class), cf).build();
        assertEquals(1, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(String.class, double[].class), varArgs.getModifiedMethodArguments());

        varArgs = new CastableTypesVarArgsBuilder(getTestArguments(BigDecimal.class, int.class, Double.class, double.class, Integer.class), cf).build();
        assertEquals(0, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(BigDecimal[].class), varArgs.getModifiedMethodArguments());

        varArgs = new CastableTypesVarArgsBuilder(getTestArguments(BigDecimal.class, int.class, Double.class, double.class, String.class), cf).build();
        assertEquals(4, varArgs.getFirstVarArgIndex());
        assertArrayEquals(getTestArguments(BigDecimal.class, int.class, Double.class, double.class, String[].class), varArgs.getModifiedMethodArguments());
}
    
    private VarArgsInfo createCastableVarArgs(CastFactory castFactory, Class<?> ... types) {
        return new CastableTypesVarArgsBuilder(getTestArguments(types), castFactory).build();
    }
    
    private static IOpenClass[] getTestArguments(IOpenClass... classes) {
        return classes;
    }

    private static IOpenClass[] getTestArguments(Class<?> ... classes) {
        List<IOpenClass> types = new ArrayList<IOpenClass>();
        for (Class<?> type : classes) {
            types.add(JavaOpenClass.getOpenClass(type));
        }
        return types.toArray(new IOpenClass[types.size()]);
    }
}
