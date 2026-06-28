package org.openl.rules.webstudio.web.trace.debug;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultSourceClassifierTest {

    private final DefaultSourceClassifier classifier = new DefaultSourceClassifier();

    @Test
    void describeConditionParsesAMatchedPerRulePut() {
        Object condition = new Object();
        ConditionCheck check = classifier.describeCondition("condition", new Object[]{condition, 2, Boolean.TRUE});
        assertSame(condition, check.condition());
        assertArrayEquals(new int[]{2}, check.rules());
        assertTrue(check.successful());
    }

    @Test
    void describeConditionMarksUnmatchedChecks() {
        ConditionCheck check = classifier.describeCondition("condition", new Object[]{new Object(), 5, Boolean.FALSE});
        assertFalse(check.successful());
        assertArrayEquals(new int[]{5}, check.rules());
    }

    @Test
    void describeConditionIgnoresNonConditionPuts() {
        assertNull(classifier.describeCondition("cell", new Object[]{new Object(), 1, Boolean.TRUE}));
        assertNull(classifier.describeCondition("result", new Object[]{new Object()}));
    }

    @Test
    void describeConditionIgnoresMalformedArgs() {
        assertNull(classifier.describeCondition("condition", new Object[]{new Object()}));
        assertNull(classifier.describeCondition("condition", new Object[]{new Object(), "x", Boolean.TRUE}));
    }
}
