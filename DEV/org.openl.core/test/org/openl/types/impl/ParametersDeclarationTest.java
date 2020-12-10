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
        assertEquals(paramDecl1, paramDecl2);
        assertNotEquals(paramDecl1, paramDecl3);
    }

    @Test
    public void testHashCode() {
        // same hash codes for equal objects
        assertEquals(paramDecl1.hashCode(), paramDecl2.hashCode());
        // not the same hash codes for unequal objects
        assertNotEquals(paramDecl1.hashCode(), paramDecl3.hashCode());
    }

}
