/*
 * Created on Aug 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

import org.junit.Assert;
import org.openl.grammar.JavaCC30Position;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TextIntervalTest extends TestCase {

    /**
     * Constructor for TextIntervalTest.
     *
     * @param name
     */
    public TextIntervalTest(String name) {
        super(name);
    }

    public void testJavaCC30Position() {
        String text = "abc\r\nedf\r\n";

        JavaCC30Position pos = new JavaCC30Position(1, 1);

        TextInfo ti = new TextInfo(text);

        int abspos = pos.getAbsolutePosition(ti);

        Assert.assertEquals(0, abspos);

    }

}
