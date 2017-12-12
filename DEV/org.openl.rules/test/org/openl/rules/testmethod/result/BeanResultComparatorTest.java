package org.openl.rules.testmethod.result;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class BeanResultComparatorTest {

    @Test
    public void test() throws NoSuchFieldException {
        BeanResultComparator comparator = new BeanResultComparator(Collections.<IOpenField> emptyList());
        assertTrue(comparator.isEqual(null, null));

        List<IOpenField> fields = new ArrayList<IOpenField>(JavaOpenClass.STRING.getFields().values());
        BeanResultComparator comparator1 = new BeanResultComparator(fields);

        assertFalse(comparator1.isEqual("Hello", null));
        assertFalse(comparator1.isEqual(null, "Hello"));
        assertTrue(comparator1.isEqual("Hello", "Hello"));
        assertTrue(comparator1.isEqual(new String("Hello"), new String("Hello")));
        assertFalse(comparator1.isEqual("By-by", "Hello"));
    }
}
