package org.openl.rules.project.instantiation;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Test;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;

public class RulesServiceEnhancerTest {

    public interface ITest {
        String hello1(int hour);
    }

    @Test
    public void dynamicWrapperEnhancementTest1() throws Exception {

        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test/resources/excel/Rules.xls"));
        module.setClassname(ITest.class.getName());
        EngineFactoryInstantiationStrategy strategy = new EngineFactoryInstantiationStrategy(module);

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        Method method = serviceClass.getMethod("hello1", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });

        assertEquals("Good Morning, World!", (String) result);

        context.setCountry(CountriesEnum.RU);
        result = method.invoke(instance, new Object[] { context, 22 });

        assertEquals("(RU) Good Night, World!", (String) result);
    }

    @Test
    public void staticWrapperEnhancementTest1() throws Exception {

        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        project.setProjectFolder(new File("."));
        Module module = new Module();
        module.setProject(project);
        module.setClassname(StaticWrapper.class.getName());

        WrapperAdjustingInstantiationStrategy strategy = new WrapperAdjustingInstantiationStrategy(module);

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        Method method = serviceClass.getMethod("hello1", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });

        assertEquals("Good Morning, World!", (String) result);

        context.setCountry(CountriesEnum.RU);
        result = method.invoke(instance, new Object[] { context, 22 });

        assertEquals("(RU) Good Night, World!", (String) result);
    }
    
    @Test
    public void apiWrapperEnhancementTest1() throws Exception {

        ProjectDescriptor project = new ProjectDescriptor();
        project.setClasspath(new ArrayList<PathEntry>());
        Module module = new Module();
        module.setProject(project);
        module.setRulesRootPath(new PathEntry("test/resources/excel/Rules.xls"));
        module.setClassname("MyTestClass");

        ApiBasedEngineFactoryInstantiationStrategy strategy = new ApiBasedEngineFactoryInstantiationStrategy(module);
        
        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setCountry(CountriesEnum.US);
        Method method = serviceClass.getMethod("hello1", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });

        assertEquals("Good Morning, World!", (String) result);

        context.setCountry(CountriesEnum.RU);
        result = method.invoke(instance, new Object[] { context, 22 });

        assertEquals("(RU) Good Night, World!", (String) result);
    }
}
