package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

/**
 * Check the case when main project includes 2 dependencies. Both dependencies
 * are containing the identical datatypes. Test that error about datatype
 * duplication will be thrown.
 * 
 * @author DLiauchuk
 *
 */
public class DuplicateDatatypesTest {

    @Test
    public void test() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test/resources/dependencies/testDuplicateDatatypes").build();
        try {
            factory.getCompiledOpenClass();
            fail("Should throw exception, as there is datatype duplication");
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(e.getCause().getCause().getMessage().contains("The type TestType2 has been already defined"));
        }
    }
}
