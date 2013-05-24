package org.openl.util.generation;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import org.junit.Test;

public class AlphabeticalFieldNameComparatorTest {
    protected class ClassForGeneration {
        @SuppressWarnings("unused")
        private String field1;
        @SuppressWarnings("unused")
        private int ddd;
        @SuppressWarnings("unused")
        private Date abra;
        @SuppressWarnings("unused")
        private String adra;
    }

    @Test
    public void testCompare() {
        Field[] fields = ClassForGeneration.class.getDeclaredFields();
        Arrays.sort(fields, new Comparator<Field>() {
            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        assertEquals("abra", fields[0].getName());
        assertEquals("adra", fields[1].getName());
        assertEquals("ddd", fields[2].getName());
        assertEquals("field1", fields[3].getName());

    }
}
