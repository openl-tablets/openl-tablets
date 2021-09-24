package org.openl.rules.lang.xls.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;
import org.openl.classloader.OpenLClassLoader;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.types.IOpenClass;
import org.openl.types.impl.ComponentTypeArrayOpenClass;

/**
 *
 * @author DLiauchuk
 *
 */
public class DatatypeOpenClassTest {

    private final static String DEFAULT_PACKAGE = "default.test";
    private final static String DEFAULT_NAME = "DatatypeTest";
    private final static String ANY_URL = "file://hello";

    private IOpenClass from;

    private static DatatypeOpenClass buildDatatypeOpenClass(String name,
            String packageName) throws ClassNotFoundException {
        DatatypeOpenClass datatypeOpenClass = new DatatypeOpenClass(name, packageName);
        OpenLClassLoader classLoader = new OpenLClassLoader(Thread.currentThread().getContextClassLoader());
        JavaBeanClassBuilder javaBeanClassBuilder = new JavaBeanClassBuilder(datatypeOpenClass.getJavaName());
        classLoader.addGeneratedClass(datatypeOpenClass.getJavaName(), javaBeanClassBuilder.byteCode());
        datatypeOpenClass.setInstanceClass(classLoader.loadClass(datatypeOpenClass.getJavaName()));
        return datatypeOpenClass;
    }

    @Before
    public void setUp() throws ClassNotFoundException {
        from = new ComponentTypeArrayOpenClass(buildDatatypeOpenClass("MyType", "org.openl.generated.packA"));
    }

    @Test
    public void testEquals() throws ClassNotFoundException {
        DatatypeOpenClass doc1 = buildDatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc1.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));

        DatatypeOpenClass doc2 = buildDatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc2.setInstanceClass(doc1.getInstanceClass());
        doc2.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));

        DatatypeOpenClass doc3 = buildDatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc3.setInstanceClass(doc1.getInstanceClass());
        doc3.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));
        // reflexive check
        //
        assertEquals(doc1, doc1);
        assertEquals(doc1.hashCode(), doc1.hashCode());

        // symmetric check
        //
        assertEquals(doc1, doc2);
        assertEquals(doc2, doc1);
        assertEquals(doc1.hashCode(), doc2.hashCode());

        // transitive check
        //
        assertEquals(doc1, doc2);
        assertEquals(doc2, doc3);
        assertEquals(doc3, doc1);

        // consistent check
        //
        assertEquals(doc1, doc2);
        assertEquals(doc1, doc2);
        assertEquals(doc1, doc2);

        // null check
        //
        assertNotEquals(null, doc1);

        DatatypeOpenClass doc4 = buildDatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE + "suffix");
        assertNotEquals(doc1, doc4);
        assertNotEquals(doc4, doc1);
        assertNotEquals(doc1.hashCode(), doc4.hashCode());
    }

    @Test
    public void testEquals_ComponentTypeArrayOpenClass_componentClassWithDiffPackages() throws ClassNotFoundException {
        IOpenClass to = new ComponentTypeArrayOpenClass(buildDatatypeOpenClass("MyType", "org.openl.generated.packB"));
        assertNotEquals(from, to);
    }

    @Test
    public void testEquals_ComponentTypeArrayOpenClass_componentClassWithSamePackages() throws ClassNotFoundException {
        DatatypeOpenClass datatypeOpenClass = buildDatatypeOpenClass("MyType", "org.openl.generated.packA");
        IOpenClass to = new ComponentTypeArrayOpenClass(datatypeOpenClass);
        assertNotEquals(from, to);
        datatypeOpenClass = buildDatatypeOpenClass("MyType", "org.openl.generated.packA");
        datatypeOpenClass.setInstanceClass(from.getComponentClass().getInstanceClass());
        to = new ComponentTypeArrayOpenClass(datatypeOpenClass);
        assertEquals(from, to);
    }

    @Test
    public void test_toString() {
        assertEquals("[Lorg.openl.generated.packA.MyType;", from.toString());
    }
}
