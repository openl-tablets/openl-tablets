package org.openl.binding.impl;

import java.io.Serializable;
import java.math.BigDecimal;

import org.junit.Test;
import org.openl.binding.ICastFactory;
import org.openl.conf.ConfigurableResourceContext;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeCastFactory;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

import static org.junit.Assert.*;

public class MethodSearchTest {

    public static class ClassWithMethods {
        public void method1(int arg1, double arg2) {
        };

        public void method1(int arg1, BigDecimal arg2) {
        };

        public void method2(int arg1, double arg2) {
        };
    }

    public static class SecondClassWithMethods extends ClassWithMethods implements Serializable {
        public void method1(int arg1, double arg2) {
        };
        public void method3(int arg1, double arg2) {
        };
    }

    public static class ThirdClassWithMethods extends SecondClassWithMethods {
    }    
    
    private ICastFactory getCastFactory() {
        TypeCastFactory typecast = new TypeCastFactory();
        TypeCastFactory.JavaCastComponent javacast = new TypeCastFactory.JavaCastComponent();
        javacast.setLibraryClassName(org.openl.binding.impl.Operators.class.getName());
        javacast.setClassName(org.openl.binding.impl.cast.CastFactory.class.getName());
        typecast.addJavaCast(javacast);
        OpenLConfiguration openLConfiguration = new OpenLConfiguration();
        openLConfiguration.setTypeCastFactory(typecast);
        openLConfiguration.setConfigurationContext(new ConfigurableResourceContext(null));
        return openLConfiguration;
    }

    @Test
    public void testMethodChoosing() {
        ICastFactory castFactory = getCastFactory();

        JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(ClassWithMethods.class);
        JavaOpenClass javaOpenClass2 = JavaOpenClass.getOpenClass(SecondClassWithMethods.class);

        assertEquals(javaOpenClass.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }),
            MethodSearch.getMethodCaller("method1",
                new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.INT },
                castFactory,
                javaOpenClass));
        assertEquals(javaOpenClass2.getMethod("method1", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }),
            MethodSearch.getMethodCaller("method1",
                new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.INT },
                castFactory,
                javaOpenClass2));

    }

    @Test
    public void testMethodInheritance() {
        ICastFactory castFactory = getCastFactory();
        
        JavaOpenClass javaOpenClass = JavaOpenClass.getOpenClass(ClassWithMethods.class);
        JavaOpenClass javaOpenClass2 = JavaOpenClass.getOpenClass(SecondClassWithMethods.class);
        JavaOpenClass javaOpenClass3 = JavaOpenClass.getOpenClass(ThirdClassWithMethods.class);
        assertNotNull(javaOpenClass.getMethod("method2", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));
        assertNotNull(javaOpenClass2.getMethod("method2", new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE }));
        
        assertNotNull(MethodSearch.getMethodCaller("method2",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE },
            castFactory,
            javaOpenClass2));     

        assertNotNull(MethodSearch.getMethodCaller("method3",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE },
            castFactory,
            javaOpenClass2));     

        assertNotNull(MethodSearch.getMethodCaller("method3",
            new IOpenClass[] { JavaOpenClass.INT, JavaOpenClass.DOUBLE },
            castFactory,
            javaOpenClass3));     
    }

}
