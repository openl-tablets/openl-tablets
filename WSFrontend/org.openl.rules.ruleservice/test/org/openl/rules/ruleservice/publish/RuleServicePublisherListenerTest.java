package org.openl.rules.ruleservice.publish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.management.ServiceManager;
import org.openl.rules.ruleservice.servlet.ServiceInfoProvider;

@TestPropertySource(properties = {"production-repository.uri=test-resources/RulesPublisherTest",
  "ruleservice.isProvideRuntimeContext=false",
  "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml", "classpath:RuleServicePublisherListenerTest.xml"})
public class RuleServicePublisherListenerTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void test() throws Exception {
        assertNotNull(applicationContext);
        ServiceInfoProvider serviceManager = applicationContext.getBean("serviceManager", ServiceInfoProvider.class);
        assertNotNull(serviceManager);
        ServiceManager publisher = applicationContext.getBean("serviceManager", ServiceManager.class);

        assertFalse(serviceManager.getServicesInfo().isEmpty());

        OpenLService service = publisher.getServiceByDeploy("RulesPublisherTest/org.openl.tablets.tutorial4");

        assertEquals(2, RuleServicePublisherListenerTestListener.onDeployCount);
        assertEquals(0, RuleServicePublisherListenerTestListener.onUndeployCount);

        publisher.undeploy("RulesPublisherTest/org.openl.tablets.tutorial4");
        publisher.deploy(service);

        assertEquals(3, RuleServicePublisherListenerTestListener.onDeployCount);
        assertEquals(1, RuleServicePublisherListenerTestListener.onUndeployCount);

        publisher.undeploy("RulesPublisherTest/org.openl.tablets.tutorial4");

        assertEquals(3, RuleServicePublisherListenerTestListener.onDeployCount);
        assertEquals(2, RuleServicePublisherListenerTestListener.onUndeployCount);

    }
}
