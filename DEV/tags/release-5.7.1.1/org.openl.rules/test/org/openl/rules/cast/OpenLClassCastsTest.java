package org.openl.rules.cast;

import static org.junit.Assert.*;
import org.junit.Test;
import org.openl.OpenL;
import org.openl.binding.ICastFactory;
import org.openl.types.IOpenCast;
import org.openl.types.java.JavaOpenClass;

public class OpenLClassCastsTest {
    @Test
    public void testCastDistances() throws Exception {
        OpenL openL = OpenL.getInstance("org.openl.rules.java");
        ICastFactory castFactory = openL.getBinder().getCastFactory();
        JavaOpenClass integerClass = JavaOpenClass.getOpenClass(Integer.class);
        IOpenCast autoboxing = castFactory.getCast(integerClass, JavaOpenClass.INT);
        IOpenCast autoboxingWithAutocast = castFactory.getCast(integerClass, JavaOpenClass.DOUBLE);
        IOpenCast cast = castFactory.getCast(JavaOpenClass.DOUBLE, JavaOpenClass.INT);
        assertTrue(autoboxing.getDistance(integerClass, JavaOpenClass.INT) < autoboxingWithAutocast.getDistance(
                integerClass, JavaOpenClass.DOUBLE));
        assertTrue(autoboxingWithAutocast.getDistance(integerClass, JavaOpenClass.DOUBLE) < cast.getDistance(
                JavaOpenClass.DOUBLE, JavaOpenClass.INT));
    }

}
