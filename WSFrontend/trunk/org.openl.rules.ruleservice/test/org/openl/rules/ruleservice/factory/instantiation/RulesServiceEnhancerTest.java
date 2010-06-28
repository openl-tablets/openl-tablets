package org.openl.rules.ruleservice.factory.instantiation;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;

import org.junit.Test;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.ruleservice.instantiation.EngineFactoryInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.RulesServiceEnhancer;
import org.openl.rules.ruleservice.instantiation.WebServiceEngineFactoryInstantiationStrategy;
import org.openl.rules.ruleservice.instantiation.WrapperAdjustingInstantiationStrategy;

public class RulesServiceEnhancerTest {

    public interface ITest {
        String hello1(int hour);
    }

    @Test
    public void dynamicWrapperEnhancementTest1() throws Exception {

        File file = new File("test/org/openl/rules/ruleservice/factory/instantiation/Rules.xls");
        EngineFactoryInstantiationStrategy strategy = new EngineFactoryInstantiationStrategy(file, ITest.class);

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

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

        WrapperAdjustingInstantiationStrategy strategy = new WrapperAdjustingInstantiationStrategy(".",
            StaticWrapper.class);

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

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

        File file = new File("test/org/openl/rules/ruleservice/factory/instantiation/Rules.xls");
        WebServiceEngineFactoryInstantiationStrategy strategy = new WebServiceEngineFactoryInstantiationStrategy(file, "MyTestClass", this.getClass().getClassLoader());
        
        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

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
