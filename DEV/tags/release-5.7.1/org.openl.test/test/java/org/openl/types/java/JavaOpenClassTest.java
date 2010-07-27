package org.openl.types.java;

import java.net.URL;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.MethodKey;

/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

/**
 * @author snshor
 * 
 */
public class JavaOpenClassTest extends TestCase {

    public static class TestClass1 {
        static public final String staticX = "staticX";
        protected int protectedX;
        public String publicX = "publicX";

        public String getM1(int x, TestClass2 y) {
            return "m1";
        }
    }

    static class TestClass2 extends TestClass1 {
        static public String staticX2 = "staticX2";
    }

    static public class TestClass3 extends TestClass2 {
        static public String staticX3 = "staticX3";
        static public String staticX2 = "staticX2";

    }

    /**
     * Constructor for JavaOpenClassTest.
     * 
     * @param arg0
     */
    public JavaOpenClassTest(String arg0) {
        super(arg0);
    }

    void _testMethodKey() {
        Class<?>[] cc = { int.class, TestClass2.class };
        MethodKey mk1 = new MethodKey("getM1", JavaOpenClass.getOpenClasses(cc));
        MethodKey mk2 = new MethodKey("getM1", JavaOpenClass.getOpenClasses(cc));

        Assert.assertEquals(mk1, mk2);
        Assert.assertEquals(mk1.hashCode(), mk2.hashCode());
    }

    public void testCache() {
        Assert.assertEquals(JavaOpenClass.getOpenClass(int.class).nullObject(), new Integer(0));
        Assert.assertTrue(JavaOpenClass.getOpenClass(String.class) == JavaOpenClass.getOpenClass(String.class));
    }

    public void testFields() {
        JavaOpenClass oc1 = JavaOpenClass.getOpenClass(TestClass1.class);
        JavaOpenClass oc2 = JavaOpenClass.getOpenClass(TestClass2.class);
        JavaOpenClass oc3 = JavaOpenClass.getOpenClass(TestClass3.class);

        IOpenField publicX = oc1.getField("publicX");
        IOpenField staticX = oc2.getField("staticX");
        IOpenField staticX2 = oc2.getField("staticX2");
        IOpenField staticX3 = oc3.getField("staticX3");
        IOpenField staticX32 = oc3.getField("staticX2");

        Assert.assertNull(oc1.getField("protectedX"));
        Assert.assertNull(staticX2);

        Assert.assertNotNull(publicX);
        Assert.assertNotNull(staticX);

        Assert.assertEquals("staticX", staticX.get(null, null));
        Assert.assertEquals("staticX2", staticX32.get(null, null));
        Assert.assertEquals("staticX3", staticX3.get(null, null));

        Assert.assertEquals("publicX", publicX.get(new TestClass2(), null));

        Assert.assertTrue(staticX.isConst());
        Assert.assertTrue(staticX.isStatic());

    }

    public void testJavaOpenSchema() throws Exception {
        JavaOpenFactory factory = new JavaOpenFactory();

        URL url = this.getClass().getClassLoader().getResource(".");
        IOpenSchema schema = factory.getSchema(url.getPath(), false);

        IOpenClass oc = schema.getType("org.openl.types.java.JavaOpenClassTest");

        Assert.assertNotNull(oc);
    }

    public void testMethods() {
        _testMethodKey();

        JavaOpenClass oc1 = JavaOpenClass.getOpenClass(TestClass1.class);
        Class<?>[] cc = { int.class, TestClass2.class };

        IOpenMethod m1 = oc1.getMethod("getM1", JavaOpenClass.getOpenClasses(cc));

        Assert.assertNotNull(m1);

        Object[] params = { new Integer(5), null };

        Assert.assertEquals("m1", m1.invoke(new TestClass1(), params, null));

    }

}
