package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;

import org.junit.Test;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.meta.DoubleValue;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.instantiation.ApiBasedInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.SimpleXlsResolvingStrategy;
import org.openl.rules.runtime.RulesFileDependencyLoader;

public class ExternalDependenciesTest {

    @Test
    public void testDependencies1() throws Exception {
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File("test/resources/dependencies/test1/module"));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader("test/resources/dependencies/test1/module");
        
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedInstantiationStrategy s = 
            new ApiBasedInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        Class<?> interfaceClass = s.getInstanceClass();
        Method method = interfaceClass.getMethod("hello1", new Class[]{int.class});
        Object res = method.invoke(s.instantiate(), 10);
        
        assertEquals("Good Morning", res);
    }
    
    @Test
    public void testDependencies2() throws Exception {
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File("test/resources/dependencies/test2/module"));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        IDependencyLoader loader1 = new ResolvingRulesProjectDependencyLoader("test/resources/dependencies/test2/module");
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1));
        
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedInstantiationStrategy s = 
            new ApiBasedInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        Class<?> interfaceClass = s.getInstanceClass();
        Object instance = s.instantiate();
        
        Method method = interfaceClass.getMethod("hello", new Class[]{int.class});
        Object res = method.invoke(instance, 10);
        
        assertEquals("Good Morning", res);
        
        // Get policy profile.
        //
        method = interfaceClass.getMethod("getPolicyProfile4", new Class[]{});
        res = method.invoke(instance, new Object[] {});
        Object policy = ((Object[])res)[0];

        method = interfaceClass.getMethod("processPolicy", new Class[]{policy.getClass()});
        res = method.invoke(instance, new Object[] {policy});
        
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult)res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20), Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),  Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
    }

    @Test
    public void testDependencies3() throws Exception {        
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File("test/resources/dependencies/test3/module"));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        ResolvingRulesProjectDependencyLoader loader2 = new ResolvingRulesProjectDependencyLoader("test/resources/dependencies/test3/module");
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
        
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedInstantiationStrategy s = 
            new ApiBasedInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(s);
        Class<?> interfaceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("dependency2");

        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4", new Class[]{IRulesRuntimeContext.class});
        Object res = method.invoke(instance, new Object[] { context });
        Object policy = ((Object[])res)[0];

        method = interfaceClass.getMethod("processPolicy", new Class[]{IRulesRuntimeContext.class, policy.getClass()});
        res = method.invoke(instance, new Object[] {context, policy});
        
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult)res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20), Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),  Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
        
        context.setLob("main");
        
        res = method.invoke(instance, new Object[] {context, policy});
        
        spreadsheetResult = (SpreadsheetResult)res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20), Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(4970),  Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
    }

    @Test
    public void testDependencies4() throws Exception {
        ResolvingStrategy strategy = new SimpleXlsResolvingStrategy();
        ProjectDescriptor descr = strategy.resolveProject(new File("test/resources/dependencies/test4/module"));

        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();
        
        IDependencyLoader loader1 = new ResolvingRulesProjectDependencyLoader("test/resources/dependencies/test4/module");
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1));
        
        boolean executionMode = false;
        dependencyManager.setExecutionMode(executionMode);
        
        ApiBasedInstantiationStrategy s = 
            new ApiBasedInstantiationStrategy(descr.getModules().get(0), executionMode, dependencyManager);
        
        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(s);

        Class<?> interfaceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        // Get policy profile.
        //
        Method method = interfaceClass.getMethod("getPolicyProfile4", new Class[]{ IRulesRuntimeContext.class });
        Object res = method.invoke(instance, new Object[] { context });
        Object policy = ((Object[])res)[0];

        method = interfaceClass.getMethod("processPolicy", new Class[]{ IRulesRuntimeContext.class, policy.getClass()});
        res = method.invoke(instance, new Object[] { context, policy });
        
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult)res;
        assertEquals("Eligible", spreadsheetResult.getFieldValue("$Value$Eligibility"));
        assertEquals(Double.valueOf(-20), Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Score")).getValue()));
        assertEquals(Double.valueOf(2270),  Double.valueOf(((DoubleValue)spreadsheetResult.getFieldValue("$Value$Premium")).getValue()));
        
        // Creating current date value
        Calendar calendar = Calendar.getInstance();
        calendar.set(2009, 5, 15);

        // Setting current date in context, which will be used in dispatch
        context.setCurrentDate(calendar.getTime());

        method = interfaceClass.getMethod("getTestCars", new Class[]{ IRulesRuntimeContext.class });
        res = method.invoke(instance, new Object[] { context });

        Object car = ((Object[])res)[1];

        method = interfaceClass.getMethod("getTestAddresses", new Class[]{ IRulesRuntimeContext.class });
        res = method.invoke(instance, new Object[] { context });

        Object address = ((Object[])res)[4];

        method = interfaceClass.getMethod("getPriceForOrder", new Class[]{ IRulesRuntimeContext.class, car.getClass(), int.class, address.getClass() });
        res = method.invoke(instance, new Object[] { context, car, 4, address });

        assertEquals(Double.valueOf(189050), Double.valueOf (((DoubleValue) res).getValue()));
    }
}
