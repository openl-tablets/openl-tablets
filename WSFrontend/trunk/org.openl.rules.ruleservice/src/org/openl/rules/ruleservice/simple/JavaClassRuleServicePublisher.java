package org.openl.rules.ruleservice.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;

/**
 * Java class publisher. Publisher that publish service beans as object.
 * Services can be executed via RulesFrontend.
 * 
 * @author Marat Kamalov
 * 
 */
public class JavaClassRuleServicePublisher implements RuleServicePublisher {
    // private Log log = LogFactory.getLog(JavaClassRuleServicePublisher.class);

    private RulesFrontend frontend = new RulesFrontendImpl();
    private Map<String, OpenLService> runningServices = new HashMap<String, OpenLService>();

    public RulesFrontend getFrontend() {
        return frontend;
    }

    public List<OpenLService> getRunningServices() {
        return Collections.unmodifiableList(new ArrayList<OpenLService>(runningServices.values()));
    }

    public OpenLService findServiceByName(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }

        return runningServices.get(serviceName);
    }

    public OpenLService deploy(OpenLService service) throws RuleServiceDeployException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }
        try {
            frontend.registerService(service);
        } catch (Exception e) {
            throw new RuleServiceDeployException("Service deploy failed", e);
        }

        return runningServices.put(service.getName(), service);
    }

    public OpenLService redeploy(OpenLService service) throws RuleServiceRedeployException {
        if (service == null) {
            throw new IllegalArgumentException("service argument can't be null");
        }

        try {
            undeploy(service.getName());
            return deploy(service);
        } catch (RuleServiceDeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        } catch (RuleServiceUndeployException e) {
            throw new RuleServiceRedeployException("Service redeploy was failed", e);
        }

    }

    public OpenLService undeploy(String serviceName) throws RuleServiceUndeployException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName argument can't be null");
        }
        /*
         * OpenLService service = findServiceByName(serviceName); if (service ==
         * null) { throw new RuleServiceUndeployException(String.format(
         * "Service with name \"%s\" isn't deployed.", serviceName)); }
         */
        frontend.unregisterService(serviceName);

        return runningServices.remove(serviceName);
    }

    public void setFrontend(RulesFrontend frontend) {
        if (frontend == null) {
            throw new IllegalArgumentException("frontend arg can't be null");
        }
        this.frontend = frontend;
    }

}
