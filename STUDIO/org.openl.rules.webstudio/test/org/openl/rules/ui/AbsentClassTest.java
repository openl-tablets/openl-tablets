package org.openl.rules.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;

public class AbsentClassTest {

    @Test
    public void testAbsentClass() throws Exception {
        @SuppressWarnings("unused")
        SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>()
                .setProject("test/rules/LibImports")
                .build();

        CompiledOpenClass compiledOpenClass = simpleProjectEngineFactory.getCompiledOpenClass();
        assertTrue(compiledOpenClass.hasErrors());
        assertEquals(2, compiledOpenClass.getMessages().size());

        // Field dependency
        assertEquals(Severity.ERROR, compiledOpenClass.getMessages().get(0).getSeverity());
        assertEquals("Type 'C' can't be loaded because of absent type 'org.openl.rules.beans.A'.", compiledOpenClass.getMessages().get(0).getSummary());

        // Parent dependency
        assertEquals(Severity.ERROR, compiledOpenClass.getMessages().get(1).getSeverity());
        assertEquals("Type 'org.openl.rules.beans.D' can't be loaded because of absent type 'org.openl.rules.beans.A'.", compiledOpenClass.getMessages().get(1).getSummary());
    }
}
