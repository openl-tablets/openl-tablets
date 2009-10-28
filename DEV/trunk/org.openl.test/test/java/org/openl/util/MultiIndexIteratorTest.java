/*
 * Created on Apr 24, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 */
public class MultiIndexIteratorTest extends TestCase {

    /**
     * Constructor for MultiIndexIteratorTest.
     *
     * @param name
     */
    public MultiIndexIteratorTest(String name) {
        super(name);
    }

    public void testNext() {
        int[] dim = { 2, 3, 5 };
        MultiIndexIterator mii = new MultiIndexIterator(dim);

        int i = 0;
        for (; mii.hasNext(); i++) {
            mii.nextDim();
        }

        Assert.assertEquals(30, i);
    }

}
