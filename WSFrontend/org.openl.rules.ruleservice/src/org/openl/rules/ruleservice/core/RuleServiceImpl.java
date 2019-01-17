package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.ruleservice.publish.RuleServicePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of RulesService. Uses publisher and instantiation factory. Publisher is responsible for
 * service exposing. Instantiation factory is responsible for build OpenLService instances from ServiceDescription. This
 * class designed for using it from Spring.
 *
 * @author Marat Kamalov
 */
public class RuleServiceImpl implements RuleService {

    private final Logger log = LoggerFactory.getLogger(RuleServiceImpl.class);
    /**
     * Publisher.
     */
    private RuleServicePublisher ruleServicePublisher;

    /**
     * Instantiation factory.
     */
    private RuleServiceInstantiationFactory ruleServiceInstantiationFactory;

    private Map<String, ServiceDescription> mapping = new HashMap<String, ServiceDescription>();

    /**
     * {@inheritDoc}
     */
    public void redeploy(ServiceDescription serviceDescription) throws RuleServiceDeployException,
                                                                RuleServiceUndeployException {
        OpenLService service = ruleServicePublisher.getServiceByName(serviceDescription.getName());
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", serviceDescription.getName()));
        }
        try {
            ServiceDescription sd = mapping.get(serviceDescription.getName());
            if (sd == null) {
                throw new IllegalStateException("Invalid state!!!");
            }
            if (sd.getDeployment().getVersion().compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
                undeploy(service.getName());
                OpenLService openLService = ruleServiceInstantiationFactory.createService(serviceDescription);
                deploy(serviceDescription, openLService);
            }
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on redeploy service", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg must not be null.");
        }
        OpenLService service = ruleServicePublisher.getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(String.format("There is no running service '%s'", serviceName));
        }

        ServiceDescription serviceDescription = mapping.get(serviceName);
        if (serviceDescription == null) {
            throw new IllegalStateException("Illegal State!!!");
        }

        ruleServicePublisher.undeploy(serviceName);
        mapping.remove(serviceDescription.getName());
        if (ruleServiceInstantiationFactory instanceof RuleServiceOpenLServiceInstantiationFactoryImpl) { // NEED
                                                                                                          // SOME
                                                                                                          // FIX.
            ((RuleServiceOpenLServiceInstantiationFactoryImpl) ruleServiceInstantiationFactory)
                .clear(serviceDescription.getDeployment());
        }
        service.destroy();
        log.info("Service '{}' was undeployed succesfully.", service.getName());
    }

    /**
     * {@inheritDoc}
     */
    public Collection<OpenLService> getServices() {
        Collection<OpenLService> services = ruleServicePublisher.getServices();
        return new ArrayList<OpenLService>(services);
    }

    /**
     * {@inheritDoc}
     */
    public OpenLService getServiceByName(String serviceName) {
        return ruleServicePublisher.getServiceByName(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    public void deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException {
        OpenLService service = ruleServicePublisher.getServiceByName(serviceDescription.getName());
        if (service != null) {
            throw new RuleServiceDeployException(
                "The service with name '" + serviceDescription.getName() + "' has already been deployed!");
        }
        try {
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            deploy(serviceDescription, newService);
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on deploy a service.", e);
        }
    }

    private void deploy(ServiceDescription serviceDescription,
            OpenLService newService) throws RuleServiceDeployException {
        ServiceDescription sd = mapping.get(serviceDescription.getName());
        if (sd != null) {
            throw new IllegalStateException("Illegal State!!");
        }
        ruleServicePublisher.deploy(newService);
        mapping.put(serviceDescription.getName(), serviceDescription);
        log.info("Service '{}' was deployed succesfully.", serviceDescription.getName());
    }

    public RuleServicePublisher getRuleServicePublisher() {
        return ruleServicePublisher;
    }

    public void setRuleServicePublisher(RuleServicePublisher ruleServicePublisher) {
        if (ruleServicePublisher == null) {
            throw new IllegalArgumentException("ruleServicePublisher arg must not be null.");
        }
        this.ruleServicePublisher = ruleServicePublisher;
    }

    public RuleServiceInstantiationFactory getRuleServiceInstantiationFactory() {
        return ruleServiceInstantiationFactory;
    }

    public void setRuleServiceInstantiationFactory(RuleServiceInstantiationFactory ruleServiceInstantiationFactory) {
        if (ruleServiceInstantiationFactory == null) {
            throw new IllegalArgumentException("ruleServiceInstantiationFactory arg must not be null.");
        }
        this.ruleServiceInstantiationFactory = ruleServiceInstantiationFactory;
    }

}
