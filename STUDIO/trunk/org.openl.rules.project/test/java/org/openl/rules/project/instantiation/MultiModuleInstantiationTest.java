package org.openl.rules.project.instantiation;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;

public class MultiModuleInstantiationTest {

    @Test 
    public void test1() throws Exception {

        File root = new File("test/resources/multi-module-support/test1");
        MultiProjectEngineFactoryInstantiationStrategy strategy = new MultiProjectEngineFactoryInstantiationStrategy(root);

        strategy.addInitializingListener(new InitializingListener() {

            public void afterModuleLoad(Module module) {

                ITableProperties props1 = new TableProperties();
                props1.setLob("lob1");
                Map<String, Object> params1 = new HashMap<String, Object>();
                params1.put("external-module-properties", props1);

                ITableProperties props2 = new TableProperties();
                props2.setLob("lob2");
                Map<String, Object> params2 = new HashMap<String, Object>();
                params2.put("external-module-properties", props2);

                ITableProperties props3 = new TableProperties();
                props3.setLob("lob3");
                Map<String, Object> params3 = new HashMap<String, Object>();
                params3.put("external-module-properties", props3);

                if ("project1".equals(module.getProject().getName())) {
                    module.setProperties(params1);
                }

                if ("project2".equals(module.getProject().getName())) {
                    module.setProperties(params2);
                }

                if ("project3".equals(module.getProject().getName())) {
                    module.setProperties(params3);
                }
            }

        });

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setLob("lob3");
        Method method = serviceClass.getMethod("driverRiskPremium", new Class<?>[] { IRulesRuntimeContext.class, String.class });
        Object result = method.invoke(instance, new Object[] { context, "High Risk Driver" });

        assertEquals(new DoubleValue(400), result);
    }

    @Test 
    public void test2() throws Exception {

        File root = new File("test/resources/multi-module-support/test2");
        MultiProjectEngineFactoryInstantiationStrategy strategy = new MultiProjectEngineFactoryInstantiationStrategy(root);

        Class<?> serviceClass = strategy.getServiceClass();
        Object instance = strategy.instantiate(ReloadType.NO);

        Method method = serviceClass.getMethod("worldHello", new Class<?>[] { int.class });
        Object result = method.invoke(instance, new Object[] { 10 });
        assertEquals("World, Good Morning!", result);
        
        method = serviceClass.getMethod("helloWorld", new Class<?>[] { int.class });
        result = method.invoke(instance, new Object[] { 10 });
        assertEquals("Good Morning, World!", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] {});
        result = method.invoke(instance, new Object[] {});
        assertEquals(2, ((Object[]) result).length);
        
        method = serviceClass.getMethod("getData2", new Class<?>[] {});
        result = method.invoke(instance, new Object[] {});
        assertEquals(3, ((Object[]) result).length);
    }
    
    @Test 
    public void test3() throws Exception {

        File root = new File("test/resources/multi-module-support/test3");
        MultiProjectEngineFactoryInstantiationStrategy strategy = new MultiProjectEngineFactoryInstantiationStrategy(root);

        strategy.addInitializingListener(new InitializingListener() {

            public void afterModuleLoad(Module module) {

                ITableProperties props2 = new TableProperties();
                props2.setLob("lob2");
                Map<String, Object> params2 = new HashMap<String, Object>();
                params2.put("external-module-properties", props2);

                ITableProperties props3 = new TableProperties();
                props3.setLob("lob3");
                Map<String, Object> params3 = new HashMap<String, Object>();
                params3.put("external-module-properties", props3);

                if ("project1".equals(module.getProject().getName())) {
                    // base hello method used as template
                }

                if ("project2".equals(module.getProject().getName())) {
                    module.setProperties(params2);
                }

                if ("project3".equals(module.getProject().getName())) {
                    module.setProperties(params3);
                }
            }

        });

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setLob("lob2");

        Method method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("Good Morning, World!", result);
        
        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);
        
        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);
        
        context.setLob("lob3");

        method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("World, Good Morning!", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);
        
        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);

        context.setLob("lob1");

        method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("Good Morning", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);
        
        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);

    }


}
