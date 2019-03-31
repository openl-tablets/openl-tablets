package org.openl.rules.ruleservice.publish;

import static org.junit.Assert.assertNotNull;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "ruleservice.datasource.dir=test-resources/RulesPublisherTest",
        "ruleservice.datasource.deploy.clean.datasource=false",
        "ruleservice.isProvideRuntimeContext=false" })
@ContextConfiguration({ "classpath:openl-ruleservice-beans.xml", "classpath:RuleServicePublisherLIstenerTest.xml" })
public class RuleServicePublisherLIstenerTest implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void test() throws Exception {
        assertNotNull(applicationContext);
        ServiceManager serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        assertNotNull(serviceManager);
        RuleServicePublisher publisher = applicationContext.getBean("ruleServicePublisher", RuleServicePublisher.class);

        Assert.assertFalse(publisher.getServices().isEmpty());

        OpenLService service = publisher.getServices().iterator().next();

        Assert.assertEquals(2, RuleServicePublisherLIstenerTestListener.onDeployCount);
        Assert.assertEquals(0, RuleServicePublisherLIstenerTestListener.onUndeployCount);

        publisher.undeploy(service.getName());
        publisher.deploy(service);

        Assert.assertEquals(3, RuleServicePublisherLIstenerTestListener.onDeployCount);
        Assert.assertEquals(1, RuleServicePublisherLIstenerTestListener.onUndeployCount);

        publisher.undeploy(service.getName());

        Assert.assertEquals(3, RuleServicePublisherLIstenerTestListener.onDeployCount);
        Assert.assertEquals(2, RuleServicePublisherLIstenerTestListener.onUndeployCount);

    }

    public static interface SimpleInterface {
        String worldHello(int hour);
    }
}
