package org.openl.rules.webstudio.web.test.export;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.openl.types.java.JavaOpenClass;

public class FieldDescriptorTest {
    private static final A A1 = new A("name1");
    private static final A A2 = new A("name2", 1);
    private static final A A3 = new A("id2name1", 21, 22, 23);

    private static final B B1 = new B("id1");
    private static final B B2 = new B("id2", A3);
    private static final B B3 = new B("id3", A1, A2, A3);

    private JavaOpenClass aType = JavaOpenClass.getOpenClass(A.class);
    private JavaOpenClass bType = JavaOpenClass.getOpenClass(B.class);

    @Test
    public void commonCases() {
        List<FieldDescriptor> descriptors;

        assertNull(FieldDescriptor.nonEmptyFields(JavaOpenClass.getOpenClass(int.class), emptyList()));
        assertNull(FieldDescriptor.nonEmptyFields(JavaOpenClass.getOpenClass(int.class), asList(1, 2, 3)));

        descriptors = FieldDescriptor.nonEmptyFields(aType, emptyList());
        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());

        descriptors = FieldDescriptor.nonEmptyFields(aType, asList(A1, A1));
        assertNotNull(descriptors);
        assertEquals(1, descriptors.size());
        assertEquals("name", descriptors.get(0).getField().getName());
        assertNull(descriptors.get(0).getChildren());

        descriptors = FieldDescriptor.nonEmptyFields(aType, asList(A1, null, A2));
        assertNotNull(descriptors);
        assertEquals(2, descriptors.size());
        assertEquals("name", descriptors.get(0).getField().getName());
        assertNull(descriptors.get(0).getChildren());
        assertEquals("values", descriptors.get(1).getField().getName());
        assertNull(descriptors.get(1).getChildren());

        descriptors = FieldDescriptor.nonEmptyFields(bType, asList(null, B2, B1));
        assertNotNull(descriptors);
        assertEquals(2, descriptors.size());
        assertEquals("id", descriptors.get(0).getField().getName());
        assertNull(descriptors.get(0).getChildren());
        assertEquals("aValues", descriptors.get(1).getField().getName());
        List<FieldDescriptor> children = descriptors.get(1).getChildren();
        assertNotNull(children);
        assertEquals(2, children.size());
        assertEquals("name", children.get(0).getField().getName());
        assertNull(children.get(0).getChildren());
        assertEquals("values", children.get(1).getField().getName());
        assertNull(children.get(1).getChildren());
    }

    @Test
    public void arrayTypes() {
        List<FieldDescriptor> descriptors = FieldDescriptor.nonEmptyFields(bType, singletonList(B2));
        assertNotNull(descriptors);
        assertFalse(descriptors.get(0).isArray());
        assertTrue(descriptors.get(1).isArray());
    }

    @Test
    public void leafNodeCount() {
        List<FieldDescriptor> descriptors = FieldDescriptor.nonEmptyFields(bType, singletonList(B2));
        assertNotNull(descriptors);
        assertEquals(1, descriptors.get(0).getLeafNodeCount());
        assertEquals(2, descriptors.get(1).getLeafNodeCount());

        descriptors = FieldDescriptor.nonEmptyFields(bType, singletonList(new B("id", new A(null, 5))));
        assertNotNull(descriptors);
        assertEquals(1, descriptors.get(0).getLeafNodeCount());
        assertEquals(1, descriptors.get(1).getLeafNodeCount());
    }

    @Test
    public void maxArraySize() {
        List<FieldDescriptor> descriptors = FieldDescriptor.nonEmptyFields(bType, asList(B2, B1));
        assertNotNull(descriptors);
        FieldDescriptor id = descriptors.get(0);
        assertEquals(1, id.getMaxArraySize(B1));
        assertEquals(1, id.getMaxArraySize(B2));
        assertEquals(1, id.getMaxArraySize(B3));

        FieldDescriptor aValues = descriptors.get(1);
        assertEquals(1, aValues.getMaxArraySize(B1)); // Even empty array contains at least 1 row
        assertEquals(1, aValues.getMaxArraySize(B2));
        assertEquals(3, aValues.getMaxArraySize(B3));

        B composite = new B("composite", A2, A1);
        composite.setChildBValues(B1, B2);
        descriptors = FieldDescriptor.nonEmptyFields(bType, asList(B3, composite));
        assertNotNull(descriptors);
        assertEquals(3, descriptors.size());
        FieldDescriptor childBValues = descriptors.get(2);
        assertEquals(1, childBValues.getMaxArraySize(B3));
        assertEquals(2, childBValues.getMaxArraySize(composite));
    }

}