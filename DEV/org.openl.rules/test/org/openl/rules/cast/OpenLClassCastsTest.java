package org.openl.rules.cast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.java.JavaOpenClass;

public class OpenLClassCastsTest {
    private static ICastFactory castFactory;

    @BeforeClass
    public static void init() {
        OpenL openL = OpenL.getInstance(OpenL.OPENL_JAVA_NAME);
        castFactory = openL.getBinder().getCastFactory();
    }

    @Test
    public void testCastDistances() {
        JavaOpenClass integerClass = JavaOpenClass.getOpenClass(Integer.class);

        IOpenCast autoboxing = castFactory.getCast(integerClass, JavaOpenClass.INT);
        IOpenCast autoboxingWithAutocast = castFactory.getCast(integerClass, JavaOpenClass.DOUBLE);
        IOpenCast cast = castFactory.getCast(JavaOpenClass.DOUBLE, JavaOpenClass.INT);
        assertTrue(autoboxing.getDistance() < autoboxingWithAutocast.getDistance());
        assertTrue(autoboxingWithAutocast.getDistance() < cast.getDistance());
    }

    @Test
    public void testBoxingUpCast() {
        JavaOpenClass comparableClass = JavaOpenClass.getOpenClass(Comparable.class);
        IOpenCast cast = castFactory.getCast(JavaOpenClass.INT, comparableClass);
        assertNotNull(cast);
        assertEquals(CastFactory.JAVA_BOXING_UP_CAST_DISTANCE, cast.getDistance());

        cast = castFactory.getCast(JavaOpenClass.DOUBLE, JavaOpenClass.OBJECT);
        assertNotNull(cast);
        assertEquals(CastFactory.JAVA_BOXING_UP_CAST_DISTANCE, cast.getDistance());
    }

    @Test
    public void testCastFromPrimitiveToOtherPrimitiveWrapper() throws Exception {
        JavaOpenClass doubleWrapperClass = JavaOpenClass.getOpenClass(Double.class);

        IOpenCast autocast = castFactory.getCast(JavaOpenClass.INT, doubleWrapperClass);
        assertNotNull(autocast);

        IOpenCast autocastNoBoxing = castFactory.getCast(JavaOpenClass.INT, JavaOpenClass.DOUBLE);
        assertTrue(autocastNoBoxing.getDistance() < autocast.getDistance());
    }

}
