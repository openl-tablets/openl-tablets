package org.openl.rules.lang.xls.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.table.properties.TableProperties;

class TableVersionComparatorTest {

    private TableVersionComparator comparator;
    private TableProperties first;
    private TableProperties second;

    @BeforeEach
    void setUp() {
        comparator = TableVersionComparator.getInstance();
        first = new TableProperties();
        second = new TableProperties();
    }

    @Test
    void testCompareEqualProperties() {
        // two different instances
        first.setActive(Boolean.TRUE);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.TRUE);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.FALSE);
        second.setActive(Boolean.FALSE);
        assertEquals(0, comparator.compare(first, second));
    }

    @Test
    void testLess() {
        first.setActive(true);
        second.setActive(false);
        assertEquals(-1, comparator.compare(first, second));
    }

    @Test
    void testMore() {
        first.setActive(false);
        second.setActive(true);
        assertEquals(1, comparator.compare(first, second));
    }

    @Test
    void testCompareNullProperties() {
        first.setActive(null);
        second.setActive(Boolean.TRUE);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(Boolean.TRUE);
        second.setActive(null);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(null);
        second.setActive(null);
        assertEquals(0, comparator.compare(first, second));

        first.setActive(null);
        second.setActive(Boolean.FALSE);
        assertEquals(-1, comparator.compare(first, second));

        first.setActive(Boolean.FALSE);
        second.setActive(null);
        assertEquals(1, comparator.compare(first, second));
    }

    @Test
    void testLessByActive() {
        first.setActive(true);
        second.setActive(false);
        assertEquals(-1, comparator.compare(first, second));
    }

    @Test
    void testMoreByActive() {
        first.setActive(false);
        second.setActive(true);
        assertEquals(1, comparator.compare(first, second));
    }
}
