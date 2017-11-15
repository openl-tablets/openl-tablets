package org.openl.rules.ruleservice.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.OpenLService;
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
@TestPropertySource(properties = {
        "ruleservice.datasource.dir=test-resources/UseRuleServiceRuntimeContextPublishingTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.datasource.type = local" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml" })
public class UseRuleServiceRuntimeContextPublishingTest implements ApplicationContextAware {

    private static final String MULTI_MODULE_OVERLOADED_DYNAMIC = "multi-module-overloaded-dynamic";
    private static final String MULTI_MODULE_OVERLOADED_STATIC = "multi-module-overloaded-static";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        this.applicationContext = arg0;
    }

    @Test
    public void testDynamicInterface() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RuleServicePublisher ruleServicePublisher = applicationContext.getBean("ruleServicePublisher",
            RuleServicePublisher.class);
        assertNotNull(ruleServicePublisher);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        OpenLService service = ruleServicePublisher.getServiceByName(MULTI_MODULE_OVERLOADED_DYNAMIC);
        assertNotNull(service);
        assertNotNull(service.getServiceClass());
        IRulesRuntimeContext cxt = new DefaultRulesRuntimeContext();
        cxt.setLob("lob1_1");
        assertEquals("Hello1", frontend.execute(MULTI_MODULE_OVERLOADED_DYNAMIC, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(MULTI_MODULE_OVERLOADED_DYNAMIC, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(MULTI_MODULE_OVERLOADED_DYNAMIC, "hello", new Object[] { cxt }));

    }

    @Test
    public void testStaticInterface() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RuleServicePublisher ruleServicePublisher = applicationContext.getBean("ruleServicePublisher",
            RuleServicePublisher.class);
        assertNotNull(ruleServicePublisher);
        RulesFrontend frontend = applicationContext.getBean("frontend", RulesFrontend.class);
        assertNotNull(frontend);
        OpenLService service = ruleServicePublisher.getServiceByName(MULTI_MODULE_OVERLOADED_STATIC);
        assertNotNull(service);
        assertNotNull(service.getServiceClass());
        IRulesRuntimeContext cxt = new DefaultRulesRuntimeContext();

        cxt.setLob("lob1_1");
        assertEquals("Hello1", frontend.execute(MULTI_MODULE_OVERLOADED_STATIC, "hello", new Object[] { cxt }));
        cxt.setLob("lob2_1");
        assertEquals("Hello2", frontend.execute(MULTI_MODULE_OVERLOADED_STATIC, "hello", new Object[] { cxt }));
        cxt.setLob("lob3_1");
        assertEquals("Hello3", frontend.execute(MULTI_MODULE_OVERLOADED_STATIC, "hello", new Object[] { cxt }));
    }

}
