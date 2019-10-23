package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.runtime.IEngineWrapper;

public class ApiInstantiationTest {

    @Test
    public void testXlsWithErrors() throws ClassNotFoundException {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setName("project1");
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test-resources/excel/"));
        Module module = new Module();
        module.setName("Rules2");
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test-resources/excel/Rules2.xls"));
        project.setModules(Collections.singletonList(module));

        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
            .singletonList(project), null, true, false, null);
        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, dependencyManager, false);

        assertNull(strategy.getServiceClass());
        try {
            assertNotNull(strategy.compile());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testInterface() throws Exception {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setName("project1");
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test-resources/excel/"));
        Module module = new Module();
        module.setName("Rules");
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test-resources/excel/Rules.xls"));
        project.setModules(Collections.singletonList(module));

        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
            .singletonList(project), null, true, false, null);
        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, dependencyManager, false);
        strategy.setServiceClass(ServiceClass.class);
        Object instance = strategy.instantiate();
        assertNotNull(instance);
        assertTrue(instance instanceof ServiceClass);
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        ((IEngineWrapper) instance).getRuntimeEnv().setContext(context);
        assertEquals("Good Evening, World!", ((ServiceClass) instance).hello1(19));
    }

    public interface ServiceClass {
        String hello1(int hour);
    }
}
