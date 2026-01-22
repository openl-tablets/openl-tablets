package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.openl.classloader.OpenLClassLoader;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class ApiInstantiationTest {

    @Test
    public void testXlsWithErrors() {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setName("project1");
        project.setClasspath(new ArrayList<>());
        project.setProjectFolder(new File("test-resources/excel/").toPath());
        Module module = new Module();
        module.setName("Rules2");
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("Rules2.xls"));
        project.setModules(Collections.singletonList(module));

        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
                null,
                false,
                null);
        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, dependencyManager, new OpenLClassLoader(Thread.currentThread().getContextClassLoader()), false);
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
        project.setClasspath(new ArrayList<>());
        project.setProjectFolder(new File("test-resources/excel/").toPath());
        Module module = new Module();
        module.setName("Rules");
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("Rules.xls"));
        project.setModules(Collections.singletonList(module));

        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections.singletonList(project),
                null,
                false,
                null);
        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, dependencyManager, ServiceClass.class.getClassLoader(), false);
        strategy.setServiceClass(ServiceClass.class);
        Object instance = strategy.instantiate();
        assertNotNull(instance);
        assertInstanceOf(ServiceClass.class, instance);
        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        assertEquals("Good Evening, World!", ((ServiceClass) instance).hello1(context, 19));
    }

    public interface ServiceClass {
        String hello1(IRulesRuntimeContext context, int hour);
    }
}
