package org.open.rules.model.scaffolding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.openl.rules.model.scaffolding.StepModel;

public class StepModelTest {

    @Test
    public void testStepModel() {
        StepModel numStep = new StepModel("num", "String", "calculation.", "0");
        StepModel oneMoreNumStep = new StepModel("num", "String", "calculation.", "0");
        StepModel sumStep = new StepModel("sum", "String", "calculation.", "0");
        StepModel doubleNumStep = new StepModel("num", "Double", "calculation.", "0");
        StepModel numWithoutDescriptionStep = new StepModel("num", "String", "", "0");
        StepModel numWithValueStep = new StepModel("num", "String", "calculation.", "1");

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

        StepModel probeStep = new StepModel();
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
