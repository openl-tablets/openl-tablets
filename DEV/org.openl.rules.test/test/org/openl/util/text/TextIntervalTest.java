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
class TextIntervalTest {

    @Test
    void testJavaCC30Position() {
        var text = "abc\r\nedf\r\n";

        var pos = new JavaCC30Position(1, 1);

        var ti = new TextInfo(text);

        var abspos = pos.getAbsolutePosition(ti);

        assertEquals(0, abspos);

    }

}
