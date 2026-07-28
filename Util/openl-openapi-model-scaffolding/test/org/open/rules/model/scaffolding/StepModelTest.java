package org.open.rules.model.scaffolding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import org.openl.rules.model.scaffolding.StepModel;

class StepModelTest {

    @Test
    void testStepModel() {
        var numStep = new StepModel("num", "String", "calculation.", "0");
        var oneMoreNumStep = new StepModel("num", "String", "calculation.", "0");
        var sumStep = new StepModel("sum", "String", "calculation.", "0");
        var doubleNumStep = new StepModel("num", "Double", "calculation.", "0");
        var numWithoutDescriptionStep = new StepModel("num", "String", "", "0");
        var numWithValueStep = new StepModel("num", "String", "calculation.", "1");

        assertEquals(numStep, numStep);
        assertNotEquals(numStep, null);

        assertEquals(numStep, oneMoreNumStep);
        assertEquals(numStep.hashCode(), oneMoreNumStep.hashCode());

        assertNotEquals(numStep, sumStep);
        assertNotEquals(numStep.hashCode(), sumStep.hashCode());

        assertNotEquals(numStep, doubleNumStep);
        assertNotEquals(numStep.hashCode(), doubleNumStep.hashCode());

        assertNotEquals(numStep, numWithoutDescriptionStep);
        assertNotEquals(numStep.hashCode(), numWithoutDescriptionStep.hashCode());

        assertNotEquals(numStep, numWithValueStep);
        assertNotEquals(numStep.hashCode(), numWithValueStep.hashCode());

        var probeStep = new StepModel();
        probeStep.setName("probeStep");
        probeStep.setType("Object");
        probeStep.setValue("=new Object()");
        probeStep.setDescription("test");

        assertEquals("probeStep", probeStep.getName());
        assertEquals("Object", probeStep.getType());
        assertEquals("test", probeStep.getDescription());
        assertEquals("=new Object()", probeStep.getValue());
    }
}
