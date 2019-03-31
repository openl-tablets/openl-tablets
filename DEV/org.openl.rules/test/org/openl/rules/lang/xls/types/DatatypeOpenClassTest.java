package org.openl.rules.lang.xls.types;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openl.types.IOpenClass;
import org.openl.types.impl.ComponentTypeArrayOpenClass;

/**
 * 
 * @author DLiauchuk
 *
 */
public class DatatypeOpenClassTest {

    private final String DEFAULT_PACKAGE = "default.test";
    private final String DEFAULT_NAME = "DatatypeTest";
    private final String ANY_URL = "file://hello";

    private IOpenClass from;

    @Before
    public void setUp() {
        from = new ComponentTypeArrayOpenClass(new DatatypeOpenClass("MyType", "org.openl.generated.packA"));
    }

    @Test
    public void testEquals() {
        DatatypeOpenClass doc1 = new DatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc1.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));

        DatatypeOpenClass doc2 = new DatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc2.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));

        DatatypeOpenClass doc3 = new DatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE);
        doc3.setMetaInfo(new DatatypeMetaInfo(DEFAULT_NAME, ANY_URL));
        // reflexive check
        //
        assertTrue(doc1.equals(doc1));
        assertEquals(doc1.hashCode(), doc1.hashCode());

        // symmetric check
        //
        assertTrue(doc1.equals(doc2));
        assertTrue(doc2.equals(doc1));
        assertEquals(doc1.hashCode(), doc2.hashCode());

        // transitive check
        //
        assertTrue(doc1.equals(doc2));
        assertTrue(doc2.equals(doc3));
        assertTrue(doc3.equals(doc1));

        // consistent check
        //
        assertTrue(doc1.equals(doc2));
        assertTrue(doc1.equals(doc2));
        assertTrue(doc1.equals(doc2));

        // null check
        //
        assertFalse(doc1.equals(null));

        DatatypeOpenClass doc4 = new DatatypeOpenClass(DEFAULT_NAME, DEFAULT_PACKAGE + "suffix");
        assertFalse(doc1.equals(doc4));
        assertFalse(doc4.equals(doc1));
        assertFalse(doc1.hashCode() == doc4.hashCode());
    }

    @Test
    public void testEquals_ComponentTypeArrayOpenClass_componentClassWithDiffPackages() {
        IOpenClass to = new ComponentTypeArrayOpenClass(new DatatypeOpenClass("MyType", "org.openl.generated.packB"));
        assertNotEquals(from, to);
    }

    @Test
    public void testEquals_ComponentTypeArrayOpenClass_componentClassWithSamePackages() {
        IOpenClass to = new ComponentTypeArrayOpenClass(new DatatypeOpenClass("MyType", "org.openl.generated.packA"));
        assertEquals(from, to);
    }

    @Test
    public void test_toString() {
        assertEquals("[Lorg.openl.generated.packA.MyType;", from.toString());
    }
}
