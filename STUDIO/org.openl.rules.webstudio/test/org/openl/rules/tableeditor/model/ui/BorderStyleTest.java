package org.openl.rules.tableeditor.model.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BorderStyleTest {

    private static final short[] RED = {0xFF, 0, 0};

    @ParameterizedTest
    @CsvSource({
            "THIN, solid, 1",
            "MEDIUM, solid, 2",
            "THICK, solid, 2",
            "DASHED, dashed, 1",
            "DASH_DOT, dashed, 1",
            "DASH_DOT_DOT, dashed, 1",
            "MEDIUM_DASHED, dashed, 2",
            "MEDIUM_DASH_DOT, dashed, 2",
            "MEDIUM_DASH_DOT_DOT, dashed, 2",
            "DOTTED, dotted, 1",
            "HAIR, dotted, 1",
            "DOUBLE, double, 1",
            // A line the screen has no style for is drawn thin.
            "SLANTED_DASH_DOT, solid, 1"
    })
    void drawsALineOfTheWorkbookInItsColour(org.apache.poi.ss.usermodel.BorderStyle line, String style, int width) {
        var drawn = BorderStyle.of(line, RED);

        assertEquals(style, drawn.getStyle());
        assertEquals(width, drawn.getWidth());
        assertArrayEquals(RED, drawn.getRgb());
    }

    @Test
    void drawsNoLineForASideWithoutBorder() {
        assertSame(BorderStyle.NONE, BorderStyle.of(org.apache.poi.ss.usermodel.BorderStyle.NONE, RED));
    }
}
