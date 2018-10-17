package org.openl.rules.tableeditor.renderkit;

import static org.junit.Assert.*;

import org.junit.Test;

public class HTMLRendererTest {

    @Test
    public void getMaxNumRowsToDisplay() {
        assertEquals(HTMLRenderer.ALL_ROWS, HTMLRenderer.getMaxNumRowsToDisplay(5, 12));
        assertEquals(HTMLRenderer.ALL_ROWS, HTMLRenderer.getMaxNumRowsToDisplay(417, 12));
        assertEquals(417, HTMLRenderer.getMaxNumRowsToDisplay(418, 12));
    }
}