package org.openl.rules.ruleservice.multimodule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.openl.rules.ruleservice.management.ServiceManager;

/**
 * OpenL Rule Services with a single publisher publishes a project with empty {@code <publishers/>} too, so the
 * service of the project gets its own Spring configuration. The Rule Services of the web application, with several
 * publishers, publish such a project nowhere, which the smoke integration tests check.
 */
@TestPropertySource(properties = {"production-repository.uri=test-resources/SinglePublisherSpringConfigTest",
        "production-repository.factory = repo-file"})
@SpringJUnitConfig(locations = {"classpath:openl-ruleservice-beans.xml"})
class SinglePublisherSpringConfigTest {

    @Autowired
    private ServiceManager serviceManager;

    @Test
    void publishedProjectWithEmptyPublishersGetsItsConfiguration() throws Exception {
        var service = serviceManager.getServiceByDeploy("SinglePublisherSpringConfigTest/stub");
        assertNotNull(service, "The service is not deployed");
        service.getServiceBean();
        var serviceContext = service.getServiceContext();
        assertNotNull(serviceContext, "The service has no Spring context");
        assertEquals("stub", serviceContext.getBean("springConfigOwner"));
    }
}
