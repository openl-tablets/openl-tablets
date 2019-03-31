package org.openl.types.impl;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.openl.types.java.JavaOpenClass;

public class ParametersDeclarationTest {

    ParameterDeclaration paramDecl1;
    ParameterDeclaration paramDecl2;
    ParameterDeclaration paramDecl3;

    @Before
    public void init() {
        String name1 = "paramDeclaration1";
        String name2 = "paramDeclaration3";
        paramDecl1 = new ParameterDeclaration(JavaOpenClass.BOOLEAN, name1);
        paramDecl2 = new ParameterDeclaration(JavaOpenClass.BOOLEAN, name1);
        paramDecl3 = new ParameterDeclaration(JavaOpenClass.BOOLEAN, name2);
    }

    @Test
    public void testEquals() {
        assertTrue(paramDecl1.equals(paramDecl2));
        assertFalse(paramDecl1.equals(paramDecl3));
    }

    @Test
    public void testHashCode() {
        // same hash codes for equal objects
        assertEquals(paramDecl1.hashCode(), paramDecl2.hashCode());
        // not the same hash codes for unequal objects
        assertFalse(paramDecl1.hashCode() == paramDecl3.hashCode());
    }

}
