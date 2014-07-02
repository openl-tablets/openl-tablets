package org.openl.rules.ruleservice.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:CustomSpreadsheetResultInterfaceEnchancerHelperTest/openl-ruleservice-beans.xml" })
public class CustomSpreadsheetResultInterfaceEnchancerHelperTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testInstantiation() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        Object result = frontend.execute("CustomSpreadsheetResultInterfaceEnchancerHelperTest_TestingSpreadsheet",
            "test",
            "conv1",
            10,
            1.5d);

        assertTrue("CustomSpreadSheet should be returned by service bean!",
            (result instanceof SpreadsheetResult) && !SpreadsheetResult.class.equals(result.getClass()) && result.getClass().getCanonicalName()
                .equals(CustomSpreadsheetResultInterfaceEnchancerHelper.CUSTOMSPREADSHEETRESULT_PREFIX + "test"));

        Class<?> serviceClass = frontend.findServiceByName("CustomSpreadsheetResultInterfaceEnchancerHelperTest_TestingSpreadsheet")
            .getServiceClass();

        for (Method method : serviceClass.getMethods()) {
            Class<?> returnType = method.getReturnType();
            if (SpreadsheetResult.class.isAssignableFrom(returnType) && !SpreadsheetResult.class.equals(returnType)) {
                if (returnType.getCanonicalName()
                    .equals(CustomSpreadsheetResultInterfaceEnchancerHelper.CUSTOMSPREADSHEETRESULT_PREFIX + method.getName())) {
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
