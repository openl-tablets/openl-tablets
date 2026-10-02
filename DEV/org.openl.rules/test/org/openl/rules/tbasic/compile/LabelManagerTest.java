package org.openl.rules.tbasic.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LabelManagerTest {

    private static final String LOOP_END = "gen_label_end_loop";

    @Test
    void findsLoopLabelOfInnermostEnclosingLoop() {
        var labels = new LabelManager();
        labels.startOperationsSet(true);
        var outerLoopEnd = labels.getLabelByInstruction(LOOP_END);
        labels.startOperationsSet(true);
        var innerLoopEnd = labels.getLabelByInstruction(LOOP_END);
        labels.startOperationsSet(false);
        labels.startOperationsSet(false);

        assertNotEquals(outerLoopEnd, innerLoopEnd);
        assertEquals(innerLoopEnd, labels.getLabelByInstruction(LOOP_END));

        labels.finishOperationsSet();
        labels.finishOperationsSet();
        labels.finishOperationsSet();
        labels.startOperationsSet(false);

        assertEquals(outerLoopEnd, labels.getLabelByInstruction(LOOP_END));
    }

    @Test
    void failsOnLoopLabelOutsideOfLoop() {
        var labels = new LabelManager();
        labels.startOperationsSet(false);

        var e = assertThrows(IllegalStateException.class, () -> labels.getLabelByInstruction(LOOP_END));
        assertEquals("No enclosing operation has the 'end' label.", e.getMessage());
    }

    @Test
    void failsOnInstructionThatIsNotLabel() {
        var labels = new LabelManager();
        labels.startOperationsSet(false);

        var e = assertThrows(IllegalArgumentException.class, () -> labels.getLabelByInstruction("end"));
        assertEquals("'end' is not a label instruction.", e.getMessage());
    }

    @Test
    void failsOnLabelInstructionWithUnknownSuffix() {
        var labels = new LabelManager();
        labels.startOperationsSet(false);

        var e = assertThrows(IllegalArgumentException.class,
                () -> labels.getLabelByInstruction("gen_label_end_next"));
        assertEquals("The label instruction 'gen_label_end_next' must name a label, optionally followed by '_loop'.",
                e.getMessage());
    }
}
