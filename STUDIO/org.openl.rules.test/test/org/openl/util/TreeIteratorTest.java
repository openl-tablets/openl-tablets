/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;

import org.junit.Assert;
import org.openl.util.tree.TreeIterator;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TreeIteratorTest extends TestCase {

    String[] x1 = { "aaa", "bbb", "ccc" };

    String[] x2 = { "ddd", "eee", "fff" };

    String[] x3 = { "ggg", "hhh" };

    String[][] root = { x1, x2, x3 };

    /**
     * Constructor for TreeIteratorTest.
     *
     * @param arg0
     */
    public TreeIteratorTest(String arg0) {
        super(arg0);
    }

    TreeIterator create(int mode) {
        return new TreeIterator(root, new TreeIterator.TreeAdaptor() {
            public Iterator children(Object node) {
                if (node.getClass().isArray()) {
                    return OpenIterator.fromArray((Object[]) node);
                }
                return null;
            }
        }, mode);
    }

    public void testCount() {
        TreeIterator it = create(TreeIterator.DEFAULT);
        int size = 0;
        while (it.hasNext()) {
            it.next();
            size++;
        }
        Assert.assertEquals(12, size);
    }

    /*
     * Test for int size()
     */
    public void testSize() {
        TreeIterator it = create(TreeIterator.DEFAULT);
        Assert.assertEquals(-1, it.size());

    }
}
