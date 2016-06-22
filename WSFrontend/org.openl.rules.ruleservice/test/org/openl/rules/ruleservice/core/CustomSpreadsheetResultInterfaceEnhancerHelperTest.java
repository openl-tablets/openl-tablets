package org.openl.rules.ruleservice.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties={"ruleservice.isProvideRuntimeContext=false", "ruleservice.datasource.dir=test-resources/CustomSpreadsheetResultInterfaceEnhancerHelperTest"})
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml", "classpath:CustomSpreadsheetResultInterfaceEnhancerHelperTest.xml" })
public class CustomSpreadsheetResultInterfaceEnhancerHelperTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testInstantiation() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        
        RuleServicePublisher ruleServicePublisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);
        assertNotNull(ruleServicePublisher);
        
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        Object result = frontend.execute("CustomSpreadsheetResultInterfaceEnhancerHelperTest_TestingSpreadsheet",
            "test",
            "conv1",
            10,
            1.5d);

        assertTrue("CustomSpreadSheet should be returned by service bean!",
            (result instanceof SpreadsheetResult) && !SpreadsheetResult.class.equals(result.getClass()) && result.getClass().getCanonicalName()
                .equals(CustomSpreadsheetResultInterfaceEnhancerHelper.CUSTOMSPREADSHEETRESULT_PREFIX + "test"));

        Class<?> serviceClass = ruleServicePublisher.getServiceByName("CustomSpreadsheetResultInterfaceEnhancerHelperTest_TestingSpreadsheet")
            .getServiceClass();

        for (Method method : serviceClass.getMethods()) {
            Class<?> returnType = method.getReturnType();
            if (SpreadsheetResult.class.isAssignableFrom(returnType) && !SpreadsheetResult.class.equals(returnType)) {
                if (returnType.getCanonicalName()
                    .equals(CustomSpreadsheetResultInterfaceEnhancerHelper.CUSTOMSPREADSHEETRESULT_PREFIX + method.getName())) {
                    for (Method m : returnType.getDeclaredMethods()) {
                        if (m.getName().startsWith("get$")) {
                            fail("Custom spreadsheet result shouldn't be declared in service interface!");
                        }
                    }
                }
            }
        }
    }
}
