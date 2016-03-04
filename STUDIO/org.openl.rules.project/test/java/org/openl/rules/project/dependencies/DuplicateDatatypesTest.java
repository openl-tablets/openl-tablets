package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.ApiBasedInstantiationStrategy;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;
import org.openl.rules.runtime.RulesFileDependencyLoader;

/**
 * Check the case when main project includes 2 dependencies. Both dependencies are containing the identical datatypes.
 * Test that error about datatype duplication will be thrown.
 * 
 * @author DLiauchuk
 *
 */
public class DuplicateDatatypesTest {
	
	private static final String MODULES_FOLDER = "test/resources/dependencies/testDuplicateDatatypes";
	@Test
	public void test() throws Exception{
		ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File(MODULES_FOLDER));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader(MODULES_FOLDER);
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedInstantiationStrategy s = 
            new ApiBasedInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        CompiledOpenClass openClass = s.compile();
		try {
			openClass.throwErrorExceptionsIfAny();
			fail("Should throw exception, as there is datatype duplication");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Type org.openl.this::TestType2 has been defined already"));			
		}
	}
}
