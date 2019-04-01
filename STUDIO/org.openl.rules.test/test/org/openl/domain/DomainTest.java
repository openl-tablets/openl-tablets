/*
 * Created on May 3, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.BitSet;

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
    }

    public void testEnum() {
        String[] _week = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

        Enum<String> week = new Enum<>(_week);

        String[] _weekend = new String[] { "Sunday", "Saturday" };

        EnumDomain<String> weekend = new EnumDomain<>(week, _weekend);

        EnumDomain<String> allweek = new EnumDomain<>(week, _week);

        Assert.assertEquals(allweek, weekend.or(weekend.not()));

    }

}
