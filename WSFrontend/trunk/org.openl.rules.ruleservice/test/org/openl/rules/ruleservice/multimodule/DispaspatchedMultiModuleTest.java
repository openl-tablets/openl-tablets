package org.openl.rules.ruleservice.multimodule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.simple.MethodInvocationException;
import org.openl.rules.ruleservice.simple.RulesFrontend;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dispatched-multimodule/openl-ruleservice-beans.xml",
        "classpath:dispatched-multimodule/openl-ruleservice-loader-beans.xml",
        "classpath:dispatched-multimodule/openl-ruleservice-publisher-beans.xml" })
public class DispaspatchedMultiModuleTest implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void checkModule(RulesFrontend frontend, UsStatesEnum state, String lob) throws MethodInvocationException {
        DefaultRulesRuntimeContext context = new DefaultRulesRuntimeContext();
        context.setUsState(state);
        context.setLob(lob);
        assertEquals(state.name().charAt(0) + "_" + lob.charAt(0),
            frontend.execute("service", "someMethod", new Object[] { context }));
    }

    @Test
    public void testDispatching() throws MethodInvocationException {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        serviceManager.start();
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        checkModule(frontend, UsStatesEnum.DC, "AUTO");
        checkModule(frontend, UsStatesEnum.DC, "HOME");
        checkModule(frontend, UsStatesEnum.NY, "AUTO");
        checkModule(frontend, UsStatesEnum.NY, "HOME");
        checkModule(frontend, UsStatesEnum.MO, "AUTO");
        checkModule(frontend, UsStatesEnum.MO, "HOME");
    }

}
