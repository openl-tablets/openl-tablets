package org.openl.rules.cast;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.java.JavaOpenClass;

public class OpenLClassCastsTest {
    private static OpenL openL;
    private static ICastFactory castFactory;
    
    @BeforeClass
    public static void init(){
        openL = OpenL.getInstance("org.openl.rules.java");
        castFactory = openL.getBinder().getCastFactory();
    }
    
    @Test
    public void testCastDistances() throws Exception {
        JavaOpenClass integerClass = JavaOpenClass.getOpenClass(Integer.class);
        
        IOpenCast autoboxing = castFactory.getCast(integerClass, JavaOpenClass.INT);
        IOpenCast autoboxingWithAutocast = castFactory.getCast(integerClass, JavaOpenClass.DOUBLE);
        IOpenCast cast = castFactory.getCast(JavaOpenClass.DOUBLE, JavaOpenClass.INT);
        assertTrue(autoboxing.getDistance(integerClass, JavaOpenClass.INT) < autoboxingWithAutocast.getDistance(
                integerClass, JavaOpenClass.DOUBLE));
        assertTrue(autoboxingWithAutocast.getDistance(integerClass, JavaOpenClass.DOUBLE) < cast.getDistance(
                JavaOpenClass.DOUBLE, JavaOpenClass.INT));
    }
    
    @Test
    public void testCastFromPrimitiveToOtherPrimitiveWrapper() throws Exception {
        JavaOpenClass doubleWrapperClass = JavaOpenClass.getOpenClass(Double.class);
        
        IOpenCast autocast = castFactory.getCast(JavaOpenClass.INT, doubleWrapperClass);
        assertNotNull(autocast);
        
        IOpenCast autocastNoBoxing = castFactory.getCast(JavaOpenClass.INT, JavaOpenClass.DOUBLE);
        assertTrue(autocastNoBoxing.getDistance(JavaOpenClass.INT, JavaOpenClass.DOUBLE) < autocast.getDistance(
            JavaOpenClass.INT, doubleWrapperClass));
    }

}
