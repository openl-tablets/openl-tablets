package org.openl.studio.common.projection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FieldNodeTest {

    private final FieldProjectionSupport support = new FieldProjectionSupport();

    @Test
    void noSelectionKeepsEveryField() {
        var selection = support.parseSelection(null);

        assertTrue(selection.selects("usedBy"));
        assertTrue(selection.selects("compileStatus", "compilation", "messages", "items"));
    }

    @Test
    void aSelectionKeepsTheFieldsItNamesAndWhatTheyHold() {
        var selection = support.parseSelection("id,compileStatus(compileState,compilation)");

        assertTrue(selection.selects("id"));
        assertTrue(selection.selects("compileStatus", "compileState"));
        assertTrue(selection.selects("compileStatus", "compilation", "messages", "items"),
                "a field selected without a sub-selection is kept whole");
        assertFalse(selection.selects("usedBy"));
        assertFalse(selection.selects("compileStatus", "pendingChanges"));
    }

    @Test
    void aFieldSelectedWholeKeepsWhatASubSelectionLeavesOut() {
        var selection = support.parseSelection("compileStatus,compileStatus(compileState)");

        assertTrue(selection.selects("compileStatus", "compilation", "messages", "items"));
    }
}
