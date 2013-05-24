package org.openl.rules.testmethod.test;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.rules.testmethod.result.BeanResultComparator;

public class BeanResultComparatorTest {

    @Test
    public void test() {
        BeanResultComparator comparator = new BeanResultComparator(new ArrayList<String>());
        assertTrue(comparator.compareResult(null, null, null));

        List<String> fields = new ArrayList<String>();
        fields.add("NumberFormat");
        BeanResultComparator comparator1 = new BeanResultComparator(fields);
        assertFalse(comparator1.compareResult(null, new SimpleDateFormat(), null));

        assertFalse(comparator1.compareResult(new SimpleDateFormat(), null, null));

        assertTrue(comparator1.compareResult(new SimpleDateFormat(), new SimpleDateFormat(), null));
    }
}
