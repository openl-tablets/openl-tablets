package org.openl.rules.ruleservice.simple;

import java.util.ArrayList;
import java.util.Collection;
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

    private Map<String, OpenLService> runningServices = new HashMap<>();

    public RulesFrontend getFrontend() {
        return frontend;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OpenLService> getServices() {
        Collection<OpenLService> services = runningServices.values();
        return new ArrayList<>(services);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName can't be null.");
        return runningServices.get(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        Objects.requireNonNull(service, "service can't be null.");
        try {
            OpenLService registeredService = getServiceByName(service.getName());
            if (registeredService != null) {
                throw new RuleServiceDeployException(
                    String.format("Service '%s' is already deployed. It has been replaced with new service.",
                        service.getName()));
            }
            frontend.registerService(service);
            runningServices.put(service.getName(), service);
            log.info("Service '{}' has been succesfully deployed.", service.getName());
        } catch (Exception e) {
            throw new RuleServiceDeployException("Failed to deploy service.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName can't be null.");
        frontend.unregisterService(serviceName);
        if (runningServices.remove(serviceName) == null) {
            throw new RuleServiceUndeployException(String.format("Service '%s' hasn't been deployed.", serviceName));
        }
        log.info("Service '{}' has been succesfully undeployed.", serviceName);
    }

    public void setFrontend(RulesFrontend frontend) {
        this.frontend = Objects.requireNonNull(frontend, "frontend can't be null.");
    }

}
