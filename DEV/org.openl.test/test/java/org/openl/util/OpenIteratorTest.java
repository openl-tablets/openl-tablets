/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class OpenIteratorTest extends TestCase {

    String[] ary1 = { "aaa", "bbb", "ccc" };

    /**
     * Constructor for AOpenIteratorTest.
     *
     * @param arg0
     */
    public OpenIteratorTest(String arg0) {
        super(arg0);
    }

    public void testCount() {
        IOpenIterator<String> it = OpenIterator.fromArray(ary1).reverse();
        Assert.assertEquals(3, it.count());
    }
    
    
    public void testIsEmpty() {
        Assert.assertTrue(AOpenIterator.isEmpty(OpenIterator.fromArray(null)));
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
    }

    public void testSkip() {
    }

}
