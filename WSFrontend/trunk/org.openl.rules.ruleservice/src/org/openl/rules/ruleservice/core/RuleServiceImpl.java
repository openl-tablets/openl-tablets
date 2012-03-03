package org.openl.rules.ruleservice.core;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.ruleservice.publish.RuleServicePublisher;

/**
 * Default implementation of RulesService. Uses publisher and instantiation
 * factory. Publisher is responsible for service exposing. Instantiation factory
 * is responsible for build OpenLService instances from ServiceDescription. This
 * class designed for using it from Spring.
 * 
 * @author Marat Kamalov
 * 
 */
public class RuleServiceImpl implements RuleService {

    private Log log = LogFactory.getLog(RuleServiceImpl.class);
    /**
     * Publisher
     */
    private RuleServicePublisher ruleServicePublisher;

    /**
     * Instantiation factory.
     */
    private RuleServiceInstantiationFactory ruleServiceInstantiationFactory;

    /** {@inheritDoc} */
    public OpenLService redeploy(ServiceDescription serviceDescription) throws RuleServiceRedeployException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Redeploying service method was started. Service name is \"%s\"",
                    serviceDescription.getName()));
        }
        try {
            OpenLService service = ruleServicePublisher.redeploy(ruleServiceInstantiationFactory
                    .createService(serviceDescription));
            return service;
        } catch (RuleServiceOpenLServiceInstantiationException e) {
            throw new RuleServiceRedeployException("Failed on deploy service", e);
        }
    }

    /** {@inheritDoc} */
    public OpenLService undeploy(String serviceName) throws RuleServiceUndeployException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg can't be null");
        }
        OpenLService service = ruleServicePublisher.undeploy(serviceName);

        if (log.isInfoEnabled()) {
            log.info(String.format(String.format("Service with name=\"%s\" was undeployed", serviceName)));
        }

        return service;
    }

    /** {@inheritDoc} */
    public List<OpenLService> getRunningServices() {
        return ruleServicePublisher.getRunningServices();
    }

    /** {@inheritDoc} */
    public OpenLService findServiceByName(String serviceName) {
        return ruleServicePublisher.findServiceByName(serviceName);
    }

    /** {@inheritDoc} */
    public OpenLService deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Deploying service method was started. Service name is \"%s\"",
                    serviceDescription.getName()));
        }
        try {
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLService service = ruleServicePublisher.deploy(newService);
            if (log.isInfoEnabled()) {
                log.info(String.format("Service with name=\"%s\" deployed", newService.getName()));
            }
            return service;
        } catch (RuleServiceOpenLServiceInstantiationException e) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Deploying service method failed. Service name is \"%s\"",
                        serviceDescription.getName()));
            }
            throw new RuleServiceDeployException("Failed on deploy service", e);
        }
    }

    public RuleServicePublisher getRuleServicePublisher() {
        return ruleServicePublisher;
    }

    public void setRuleServicePublisher(RuleServicePublisher ruleServicePublisher) {
        if (ruleServicePublisher == null) {
            throw new IllegalArgumentException("ruleServicePublisher arg can't be null");
        }
        this.ruleServicePublisher = ruleServicePublisher;
    }

    public RuleServiceInstantiationFactory getRuleServiceInstantiationFactory() {
        return ruleServiceInstantiationFactory;
    }

    public void setRuleServiceInstantiationFactory(RuleServiceInstantiationFactory ruleServiceInstantiationFactory) {
        if (ruleServiceInstantiationFactory == null) {
            throw new IllegalArgumentException("ruleServiceInstantiationFactory arg can't be null");
        }
        this.ruleServiceInstantiationFactory = ruleServiceInstantiationFactory;
    }

}
