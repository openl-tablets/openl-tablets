package org.openl.rules.ruleservice.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java class publisher. Publisher that publish service beans as object. Services can be executed via RulesFrontend.
 * Adaptor.
 *
 * @author Marat Kamalov
 */
public class JavaClassRuleServicePublisher implements RuleServicePublisher {

    private final Logger log = LoggerFactory.getLogger(JavaClassRuleServicePublisher.class);

    private RulesFrontend frontend = new RulesFrontendImpl();

    private final Map<String, OpenLService> runningServices = new HashMap<>();

    public RulesFrontend getFrontend() {
        return frontend;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        return runningServices.get(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service cannot be null");
        try {
            OpenLService registeredService = frontend.findServiceByName(service.getName());
            if (registeredService != null) {
                throw new RuleServiceDeployException(
                    String.format("Service '%s' is already deployed.", service.getName()));
            }
            frontend.registerService(service);
            runningServices.put(service.getServicePath(), service);
            log.info("Service '{}' has been deployed successfully.", service.getName());
        } catch (Exception e) {
            throw new RuleServiceDeployException("Failed to deploy a service.", e);
        }
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        String serviceName = service.getServicePath();
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        frontend.unregisterService(service.getName());
        if (runningServices.remove(serviceName) == null) {
            throw new RuleServiceUndeployException(String.format("Service '%s' has not been deployed.", serviceName));
        }
        log.info("Service '{}' has been undeployed successfully.", serviceName);
    }

    public void setFrontend(RulesFrontend frontend) {
        this.frontend = Objects.requireNonNull(frontend, "frontend cannot be null");
    }

    @Override
    public String getUrl(OpenLService service) {
        return null;
    }
}
