package org.openl.rules.project.instantiation;

import static junit.framework.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Calendar;
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
    public void fakeTest() {
        
    }
    
    public void test1() throws Exception {

        File root = new File("test/resources/multi-module-support");
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
        Method method = serviceClass.getMethod("driverRiskPremium", new Class<?>[] { IRulesRuntimeContext.class,
                String.class });
        Object result = method.invoke(instance, new Object[] { context, "High Risk Driver" });

        assertEquals(new DoubleValue(400), result);
        System.out.println(result);
    }
    
    
    public void test2() throws Exception {
        final Calendar cal1 = Calendar.getInstance();
        final Calendar cal2 = Calendar.getInstance();
        final Calendar cal3 = Calendar.getInstance();
        final Calendar cal4 = Calendar.getInstance();
        cal1.set(2009, Calendar.NOVEMBER, 1, 11, 0, 0);
        cal2.set(2010, Calendar.OCTOBER, 31, 11, 0, 0);
        cal3.set(2010, Calendar.NOVEMBER, 1, 11, 0, 0);
        cal4.set(2011, Calendar.OCTOBER, 31, 11, 0, 0);

        File root = new File("D:/Work/workspace/igor/multimodule6/multimodule6");
        MultiProjectEngineFactoryInstantiationStrategy strategy =
                new MultiProjectEngineFactoryInstantiationStrategy(root);

        strategy.addInitializingListener(new InitializingListener(){
            public void afterModuleLoad(Module module) {
                ITableProperties props1 = new TableProperties();
                props1.setEffectiveDate(cal1.getTime());
                props1.setExpirationDate(cal2.getTime());
                Map<String, Object> params1 = new HashMap<String, Object>();
                params1.put("external-module-properties", props1);

                ITableProperties props2 = new TableProperties();
                props2.setEffectiveDate(cal3.getTime());
                props2.setExpirationDate(cal4.getTime());
                Map<String, Object> params2 = new HashMap<String, Object>();
                params2.put("external-module-properties", props2);

                if ("module1".equals(module.getProject().getName())) {
                    module.setProperties(params1);
                }
                if ("module2".equals(module.getProject().getName())) {
                    module.setProperties(params2);
                }
            }
        });

        RulesServiceEnhancer enhancer = new RulesServiceEnhancer(strategy);
        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate(ReloadType.NO);

        IRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        Calendar cal = Calendar.getInstance();
        cal.set(2010, Calendar.OCTOBER, 15, 11, 0, 0);
        context.setCurrentDate(cal.getTime());

        Method[] methods = serviceClass.getMethods();
        for (Method method: methods) {
            if (method.getName().equals("rateAlgorithm")) {
                Object[] params = new Object[3];
                params[0] = context;
                Class<?>[] parClasses = method.getParameterTypes();
                for (Class clz: parClasses) {
                    if (clz.getSimpleName().equals("Product")) {
                        params[1] = instantiateProduct(clz);
                    } else if (clz.getSimpleName().equals("Accident")) {
                        params[2] = instantiateAccident(clz);
                    }
                }
                Object result = method.invoke(instance, params);
                System.out.println(result);
            }
        }
    }

    private static Object instantiateProduct(Class<?> clz) throws Exception {
        Object product = clz.newInstance();
        for (Method m: clz.getMethods()) {
            if (m.getName().equals("setSchemeNo")) {
                m.invoke(product, 203);
            } else if (m.getName().equals("setProductCode")) {
                m.invoke(product, 1001);
            }
        }
        return product;
    }

    private static Object instantiateAccident(Class<?> clz) throws Exception {
        Object item = clz.newInstance();
        for (Method m: clz.getMethods()) {
            if (m.getName().equals("setSumInsured")) {
                m.invoke(item, 5000);
            } else if (m.getName().equals("setYear")) {
                m.invoke(item, 2010);
            }
        }
        return item;
    }

}
