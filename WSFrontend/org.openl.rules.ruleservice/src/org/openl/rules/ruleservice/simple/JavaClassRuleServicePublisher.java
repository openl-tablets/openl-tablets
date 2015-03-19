package org.openl.rules.ruleservice.simple;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * Java class publisher. Publisher that publish service beans as object.
 * Services can be executed via RulesFrontend. Adaptor.
 *
 * @author Marat Kamalov
 */
public class JavaClassRuleServicePublisher implements RuleServicePublisher {
    private final Logger log = LoggerFactory.getLogger(JavaClassRuleServicePublisher.class);

    private RulesFrontend frontend = new RulesFrontendImpl();

    public RulesFrontend getFrontend() {
        return frontend;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<OpenLService> getServices() {
        return Collections.unmodifiableCollection(frontend.getServices());
    }

    /**
     * {@inheritDoc}
     */
    public OpenLService getServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        return frontend.findServiceByName(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public void deploy(OpenLService service) throws RuleServiceDeployException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }
        try {
            OpenLService registeredService = getServiceByName(service.getName());
            if (registeredService != null) {
                throw new RuleServiceDeployException(String.format(
                        "Service with name \"%s\" has been already deployed. Replaced with new service.",
                        service.getName()));
            }
            frontend.registerService(service);
        } catch (Exception e) {
            throw new RuleServiceDeployException("Service deploy failed", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void redeploy(OpenLService service) throws RuleServiceRedeployException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        try {
            undeploy(service.getName());
            deploy(service);
        } catch (RuleServiceDeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        } catch (RuleServiceUndeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        OpenLService service = frontend.unregisterService(serviceName);
        if (service == null) {
            log.warn("Service with name \"{}\" hasn't been deployed.", serviceName);
        }
    }

    public void setFrontend(RulesFrontend frontend) {
        if (frontend == null) {
            throw new IllegalArgumentException("frontend arg can't be null");
        }
        this.frontend = frontend;
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }    
}
