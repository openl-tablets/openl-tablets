/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import org.junit.Assert;
import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class OpenIteratorTest extends TestCase {

    /**
     * Constructor for AOpenIteratorTest.
     *
     * @param arg0
     */
    public OpenIteratorTest(String arg0) {
        super(arg0);
    }

    public void testIsEmpty() {
        Assert.assertTrue(AOpenIterator.isEmpty(OpenIterator.fromArray(null)));
    }

}
