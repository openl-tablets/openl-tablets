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

}
