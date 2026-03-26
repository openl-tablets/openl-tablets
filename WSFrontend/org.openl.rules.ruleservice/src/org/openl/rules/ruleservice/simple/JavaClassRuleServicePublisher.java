package org.openl.rules.ruleservice.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;

/**
 * Java class publisher. Publisher that publish service beans as object. Services can be executed via RulesFrontend.
 * Adaptor.
 *
 * @author Marat Kamalov
 */
@Slf4j
public class JavaClassRuleServicePublisher implements RuleServicePublisher {


    @Getter
    @Setter
    private RulesFrontend frontend = new RulesFrontendImpl();

    private final Map<String, OpenLService> runningServices = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByDeploy(String deployPath) {
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        return runningServices.get(deployPath);
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
                        "Service '%s' is already deployed.".formatted(service.getName()));
            }
            frontend.registerService(service);
            runningServices.put(service.getDeployPath(), service);
            log.info("Service '{}' has been deployed successfully.", service.getName());
        } catch (Exception e) {
            throw new RuleServiceDeployException("Failed to deploy a service.", e);
        }
    }

    @Override
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        String deployPath = service.getDeployPath();
        Objects.requireNonNull(deployPath, "deployPath cannot be null");
        frontend.unregisterService(service.getName());
        if (runningServices.remove(deployPath) == null) {
            throw new RuleServiceUndeployException("Service '%s' has not been deployed.".formatted(deployPath));
        }
        log.info("Service '{}' has been undeployed successfully.", deployPath);
    }

    @Override
    public String getUrl(OpenLService service) {
        return null;
    }

    @Override
    public String name() {
        return "JAVA";
    }
}
