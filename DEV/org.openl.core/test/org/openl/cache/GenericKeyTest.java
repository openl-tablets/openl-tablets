package org.openl.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.openl.cache.GenericKey.getInstance;

import org.junit.Test;

public class GenericKeyTest {
    @Test
    public void equalsTest() {
        assertEquals(getInstance(null, null), getInstance(null, null));
        assertEquals(getInstance(null, "A"), getInstance(null, "A"));
        assertEquals(getInstance("A", null), getInstance("A", null));
        assertEquals(getInstance("A", "B"), getInstance("A", "B"));

        assertNotEquals(getInstance("A", null), getInstance(null, "A"));
        assertNotEquals(getInstance(null, "A"), getInstance("A", null));
        assertNotEquals(getInstance("A", null), getInstance("B", null));
        assertNotEquals(getInstance(null, "A"), getInstance(null, "B"));
        assertNotEquals(getInstance("A", "A"), getInstance("A", "B"));
        assertNotEquals(getInstance("A", "B"), getInstance("B", "B"));
    }

    public void hashCodeTest() {
        assertEquals(getInstance(null, null).hashCode(), getInstance(null, null).hashCode());
        assertEquals(getInstance(null, "A").hashCode(), getInstance(null, "A").hashCode());
        assertEquals(getInstance("A", null).hashCode(), getInstance("A", null).hashCode());
        assertEquals(getInstance("A", "B").hashCode(), getInstance("A", "B").hashCode());
    }
}
