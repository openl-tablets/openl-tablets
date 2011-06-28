/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class OpenIteratorTest extends TestCase {

    String[] ary1 = { "aaa", "bbb", "ccc" };

    String[] ary2 = { "ddd", "eee", "fff" };

    /**
     * Constructor for AOpenIteratorTest.
     *
     * @param arg0
     */
    public OpenIteratorTest(String arg0) {
        super(arg0);
    }

    public void testAppend() {
    }

    public void testCount() {
        IOpenIterator<String> it = OpenIterator.fromArray(ary1).append(OpenIterator.fromArray(ary2));
        Assert.assertEquals(6, it.count());
    }

    public void testIsEmpty() {
        Assert.assertTrue(AOpenIterator.isEmpty(OpenIterator.fromArray(null)));
    }

    public void testMerge() {

        IOpenIterator<String> it = OpenIterator.fromArray(ary1).append(OpenIterator.fromArray(ary2));

        it.skip(4);

        Assert.assertEquals("eee", it.next());
    }

    public void testModifier() {
        Integer[] x = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        IOpenIteratorExtender<Integer, Integer> mod = new IOpenIteratorExtender<Integer, Integer>() {
            public Iterator<Integer> extend(Integer obj) {
                int x = (obj).intValue();
                if (x % 3 == 0) {
                    return OpenIterator.fromArray(new Integer[] { x, x / 3, x / 3 * 2 });
                } else if (x % 2 == 0) {
                    return OpenIterator.fromArray(new Integer[] { x, x / 2 });
                } else {
                    return null;
                }
            }
        };

        Assert.assertEquals(18, OpenIterator.fromArray(x).extend(mod).count());
    }

    public void testRemove() {
    }

    public void testSingle() {
        IOpenIterator<OpenIteratorTest> it = AOpenIterator.single(this);
        Assert.assertEquals(1, it.size());
        Assert.assertEquals(this, it.next());
        Assert.assertEquals(0, it.size());
    }

    public void testSize() {
        IOpenIterator<String> it = OpenIterator.fromArray(ary1);
        Assert.assertEquals(3, it.size());
        IOpenIterator<String> it2 = OpenIterator.fromArray(ary1).append(OpenIterator.fromArray(ary2));
        Assert.assertEquals(6, it2.size());
    }

    public void testSkip() {
    }

}
