/*
 * Created on Aug 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.openl.grammar.JavaCC30Position;

/**
 * @author snshor
 */
public class TextIntervalTest {

    @Test
    public void testJavaCC30Position() {
        String text = "abc\r\nedf\r\n";

        JavaCC30Position pos = new JavaCC30Position(1, 1);

        TextInfo ti = new TextInfo(text);

        int abspos = pos.getAbsolutePosition(ti);

        assertEquals(0, abspos);

    }

}
