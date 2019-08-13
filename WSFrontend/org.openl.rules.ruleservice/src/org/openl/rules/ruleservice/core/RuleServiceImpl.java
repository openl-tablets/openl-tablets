package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

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

    private Map<String, ServiceDescription> serviceDescriptionMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void redeploy(ServiceDescription serviceDescription) throws RuleServiceDeployException,
                                                                RuleServiceUndeployException {
        OpenLService service = ruleServicePublisher.getServiceByName(serviceDescription.getName());
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", serviceDescription.getName()));
        }
        try {
            ServiceDescription sd = serviceDescriptionMap.get(serviceDescription.getName());
            if (sd == null) {
                throw new IllegalStateException("Invalid state!!!");
            }
            if (sd.getDeployment().getVersion().compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
                OpenLService openLService = ruleServiceInstantiationFactory.createService(serviceDescription);
                Lock lock = RuleServiceRedeployLock.getInstance().getWriteLock();
                try {
                    lock.lock();
                    undeploy(service.getName());
                    deploy(serviceDescription, openLService);
                } finally {
                    lock.unlock();
                }
            }
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on redeploy service", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName arg must not be null.");
        }
        OpenLService service = ruleServicePublisher.getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(String.format("There is no running service '%s'", serviceName));
        }
        try {
            OpenLServiceHolder.getInstance().setOpenLService(service);
            ServiceDescription serviceDescription = serviceDescriptionMap.get(serviceName);
            if (serviceDescription == null) {
                throw new IllegalStateException("Illegal State!!!");
            }
            try {
                ruleServicePublisher.undeploy(serviceName);
                serviceDescriptionMap.remove(serviceDescription.getName());
            } finally {
                cleanDeploymentResources(serviceDescription);
                service.destroy();
            }
            log.info("Service '{}' was undeployed succesfully.", service.getName());
        } finally {
            OpenLServiceHolder.getInstance().remove();
        }
    }

    private void cleanDeploymentResources(ServiceDescription serviceDescription) {
        boolean foundServiceWithThisDeployment = false;
        for (ServiceDescription sd : serviceDescriptionMap.values()) {
            if (sd.getDeployment().equals(serviceDescription.getDeployment())) {
                foundServiceWithThisDeployment = true;
                break;
            }
        }
        if (!foundServiceWithThisDeployment) {
            ruleServiceInstantiationFactory.clean(serviceDescription);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OpenLService> getServices() {
        Collection<OpenLService> services = ruleServicePublisher.getServices();
        return new ArrayList<>(services);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByName(String serviceName) {
        return ruleServicePublisher.getServiceByName(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException {
        OpenLService service = ruleServicePublisher.getServiceByName(serviceDescription.getName());
        if (service != null) {
            throw new RuleServiceDeployException(
                "The service with name '" + serviceDescription.getName() + "' has already been deployed!");
        }
        try {
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLServiceHolder.getInstance().setOpenLService(newService);
            deploy(serviceDescription, newService);
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on deploy a service.", e);
        } finally {
            cleanDeploymentResources(serviceDescription);
            OpenLServiceHolder.getInstance().remove();
        }
    }

    private void deploy(ServiceDescription serviceDescription,
            OpenLService newService) throws RuleServiceDeployException {
        ServiceDescription sd = serviceDescriptionMap.get(serviceDescription.getName());
        if (sd != null) {
            throw new IllegalStateException("Illegal State!!");
        }
        ruleServicePublisher.deploy(newService);
        serviceDescriptionMap.put(serviceDescription.getName(), serviceDescription);
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
