package org.openl.rules.project.instantiation;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Test;
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
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("test-resources/excel/"));
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test-resources/excel/Rules.xls"));
        ApiBasedInstantiationStrategy strategy = new ApiBasedInstantiationStrategy(module, false, null);

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(
            strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        Method method = serviceClass.getMethod("hello1", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });

        assertEquals("Good Morning, World!", (String) result);

        context.setCountry(CountriesEnum.RU);
        result = method.invoke(instance, new Object[] { context, 22 });

        assertEquals("(RU) Good Night, World!", (String) result);
    }
}
