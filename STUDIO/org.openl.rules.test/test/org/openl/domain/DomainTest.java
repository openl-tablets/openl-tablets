/*
 * Created on May 3, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.BitSet;
import java.util.Iterator;

import org.junit.Assert;

import junit.framework.TestCase;

/**
 * @author snshor
 */
public class DomainTest extends TestCase {

    /**
     * Constructor for DomainTest.
     *
     * @param name
     */
    public DomainTest(String name) {
        super(name);
    }

    public void testBitSetIterator() {
        int[] ary = { 10, 17, 31, 59, 1235 };

        BitSet bs = new BitSet();

        for (int i = 0; i < ary.length; i++) {
            bs.set(ary[i]);
        }

        BitSetIterator bsi = new BitSetIterator(bs);

        for (int i = 0; i < ary.length; i++) {
            Assert.assertTrue(bsi.hasNext());
            Assert.assertEquals(ary[i], bsi.nextInt());
        }
        Assert.assertFalse(bsi.hasNext());

        assertTrue(bsi.isResetable());
        bsi.reset();
        for (int j : ary) {
            Assert.assertEquals(j, bsi.nextInt());
        }
        Assert.assertFalse(bsi.hasNext());
    }

    public void testEnum() {
        String[] _week = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

        Enum<String> week = new Enum<>(_week);

        String[] _weekend = new String[] { "Sunday", "Saturday" };

        EnumDomain<String> weekend = new EnumDomain<>(week, _weekend);

        EnumDomain<String> allweek = new EnumDomain<>(week, _week);

        Assert.assertEquals(allweek, weekend.or(weekend.not()));

        assertEquals(_week, week.getAllObjects());
        assertTrue(week.contains("Sunday"));
        assertFalse(week.contains("foo"));
        try {
            week.getIndex("bar");
            fail("Expects exception...");
        } catch (RuntimeException e) {
            assertEquals("Object 'bar' is outside of a valid domain.", e.getMessage());
        }
        EnumDomain<String> sub = allweek.sub(weekend);
        assertEquals(5, sub.size());
        assertFalse(sub.selectObject("Sunday"));
        assertTrue(sub.selectObject("Monday"));
        assertEquals("[Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday]", sub.toString());
        Iterator<String> it = sub.iterator();
        for (int i = 0; i < 5; i++) {
            assertTrue(it.hasNext());
            assertEquals(_week[i + 1], it.next());
        }
        assertFalse(it.hasNext());
        assertSame(allweek, allweek.sub(allweek));
        assertSame(allweek, allweek.or(allweek));

        try {
            allweek.sub(new EnumDomain<>(new Enum<>(_weekend), _weekend));
            fail("Expects exception...");
        } catch (RuntimeException e) {
            assertEquals("Cannot use subsets of different domains.", e.getMessage());
        }
    }

}
