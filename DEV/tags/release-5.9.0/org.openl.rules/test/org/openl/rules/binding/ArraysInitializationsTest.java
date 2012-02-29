package org.openl.rules.binding;

import java.util.Arrays;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

import static org.junit.Assert.*;

public class ArraysInitializationsTest extends BaseOpenlBuilderHelper {

    private static String src = "test/rules/binding/ArraysInitializationsTest.xls";

    public ArraysInitializationsTest() {
        super(src);
    }

    @Test
    public void testInitializationJavaStyle() {
        assertFalse(findTable("Rules Integer[] array()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[]) invokeMethod("array"), new Integer[] { 1, 2, 3 }));
        assertFalse(findTable("Rules Integer[][] arrayTwoDims()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[][]) invokeMethod("arrayTwoDims"), new Integer[][] { { 1, 2, 3 },
                { 4, 5, 6 } }));
    }

    @Test
    public void testInitializationForLocalVar() {
        assertFalse(findTable("Method Integer[] localVarArrayInit()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[]) invokeMethod("localVarArrayInit"), new Integer[] { 1, 2 }));
        assertFalse(findTable("Method Integer[][] localVarArrayTwoDimsInit()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[][]) invokeMethod("localVarArrayTwoDimsInit"), new Integer[][] {
                { 1, 2 }, { 3, 4 } }));
    }

    @Test
    public void testSimpleInitializationForLocalVar() {
        assertFalse(findTable("Method Integer[] localVarSimpleArrayInit()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[]) invokeMethod("localVarSimpleArrayInit"), new Integer[] { 1, 2, 3 }));
        assertFalse(findTable("Method Integer[][] localVarSimpleArrayTwoDimsInit()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[][]) invokeMethod("localVarSimpleArrayTwoDimsInit"), new Integer[][] {
                { 1, 2, 3 }, { 4, 5, 6 } }));
    }

    @Test
    public void testSimpleInitializationInReturn() {
        assertFalse(findTable("Method Integer[] simpleArrayInitInReturn()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[]) invokeMethod("simpleArrayInitInReturn"), new Integer[] { 1, 2, 3 }));
        assertFalse(findTable("Method Integer[][] simpleArrayTwoDimsInitInReturn()").hasErrors());
        assertTrue(Arrays.deepEquals((Integer[][]) invokeMethod("simpleArrayTwoDimsInitInReturn"), new Integer[][] {
                { 1, 2, 3 }, { 4, 5, 6 } }));
    }
}
