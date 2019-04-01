package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

/**
 * Check the case when main project includes 2 dependencies. Both dependencies are containing the identical datatypes.
 * Test that error about datatype duplication will be thrown.
 * 
 * @author DLiauchuk
 *
 */
public class DuplicateDatatypesTest {

    @Test
    public void test() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/testDuplicateDatatypes")
            .build();
        CompiledOpenClass compiledOpenClass = factory.getCompiledOpenClass();
        assertTrue("Should be an error message, as there is datatype duplication", compiledOpenClass.hasErrors());
        boolean found = false;
        for (OpenLMessage message : compiledOpenClass.getMessages()) {
            if (message.isError() && "Type 'TestType2' has already been defined.".equals(message.getSummary())) {
                found = true;
            }
        }
        assertTrue("Message not found!", found);
    }
}
