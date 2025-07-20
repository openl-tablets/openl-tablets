package org.openl.rules.project.instantiation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class RulesServiceEnhancerTest {

    @Test
    public void dynamicWrapperEnhancementTest1() throws Exception {

        ProjectDescriptor project = new ProjectDescriptor();
        project.setName("project1");
        project.setClasspath(new ArrayList<>());
        project.setProjectFolder(new File("test-resources/excel/").toPath());
        Module module = new Module();
        module.setName("Rules");
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("Rules.xls"));
        project.setModules(Collections.singletonList(module));

        IDependencyManager dependencyManager = new SimpleDependencyManager(Collections
                .singletonList(project), null, false, null);

        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, dependencyManager, null, false);

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(
                strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        Method method = serviceClass.getMethod("hello1", IRulesRuntimeContext.class, int.class);
        Object result = method.invoke(instance, context, 10);

        assertEquals("Good Morning, World!", result);

        context.setCountry(CountriesEnum.RU);
        result = method.invoke(instance, context, 22);

        assertEquals("(RU) Good Night, World!", result);
    }
}
