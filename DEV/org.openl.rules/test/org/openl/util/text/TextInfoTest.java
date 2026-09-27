package org.openl.util.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextInfoTest {

    @Test
    void positionInsideLineBelongsToThatLine() {
        var info = new TextInfo("ab\ncd\r\nef\rgh");

        assertEquals(0, info.getLineIdx(1));
        assertEquals(1, info.getLineIdx(3));
        assertEquals(1, info.getLineIdx(4));
        assertEquals(2, info.getLineIdx(8));
        assertEquals(3, info.getLineIdx(11));
        assertEquals("cd\r\n", info.getLine(1));
        assertEquals("gh", info.getLine(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab\n", "ab\r\n", "ab\r"})
    void endOfTextAfterLineBreakStartsEmptyLine(String text) {
        var info = new TextInfo(text);

        assertEquals(1, info.getLineIdx(text.length()));
        assertEquals(text.length(), info.getPosition(1));
        assertEquals("", info.getLine(1));
    }
}
