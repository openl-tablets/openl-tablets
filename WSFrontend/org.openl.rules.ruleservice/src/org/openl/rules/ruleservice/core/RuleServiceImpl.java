package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import org.openl.rules.ruleservice.publish.RuleServiceManager;
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
    private RuleServiceManager ruleServiceManager;

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
        OpenLService service = ruleServiceManager.getServiceByName(serviceDescription.getName());
        if (service == null) {
            throw new RuleServiceUndeployException(
                String.format("There is no running service with name '%s'.", serviceDescription.getName()));
        }
        ServiceDescription sd = serviceDescriptionMap.get(serviceDescription.getName());
        if (sd == null) {
            throw new IllegalStateException("Invalid state.");
        }
        if (sd.getDeployment().getVersion().compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
            Lock lock = RuleServiceRedeployLock.getInstance().getWriteLock();
            try {
                lock.lock();
                undeploy(service.getName());
                deploy(serviceDescription);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeploy(String serviceName) throws RuleServiceUndeployException {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        OpenLService service = ruleServiceManager.getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(String.format("There is no running service '%s'", serviceName));
        }
        try {
            OpenLServiceHolder.getInstance().setOpenLService(service);
            ServiceDescription serviceDescription = serviceDescriptionMap.get(serviceName);
            if (serviceDescription == null) {
                throw new IllegalStateException("This should not happen.");
            }
            try {
                ruleServiceManager.undeploy(serviceName);
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
        Collection<OpenLService> services = ruleServiceManager.getServices();
        return new ArrayList<>(services);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OpenLService getServiceByName(String serviceName) {
        return ruleServiceManager.getServiceByName(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deploy(ServiceDescription serviceDescription) throws RuleServiceDeployException {
        OpenLService service = ruleServiceManager.getServiceByName(serviceDescription.getName());
        if (service != null) {
            throw new RuleServiceDeployException(
                "The service with name '" + serviceDescription.getName() + "' has already been deployed!");
        }
        try {
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLServiceHolder.getInstance().setOpenLService(newService);
            ServiceDescription sd = serviceDescriptionMap.get(serviceDescription.getName());
            if (sd != null) {
                throw new IllegalStateException("This should not happen.");
            }
            ruleServiceManager.deploy(newService);
            serviceDescriptionMap.put(serviceDescription.getName(), serviceDescription);
            log.info("Service '{}' has been deployed succesfully.", serviceDescription.getName());
        } catch (RuleServiceInstantiationException e) {
            throw new RuleServiceDeployException("Failed on deploy a service.", e);
        } finally {
            cleanDeploymentResources(serviceDescription);
            OpenLServiceHolder.getInstance().remove();
        }
    }

    public void setRuleServiceManager(RuleServiceManager ruleServiceManager) {
        this.ruleServiceManager = Objects.requireNonNull(ruleServiceManager, "ruleServiceManager cannot be null");
    }

    public void setRuleServiceInstantiationFactory(RuleServiceInstantiationFactory ruleServiceInstantiationFactory) {
        this.ruleServiceInstantiationFactory = Objects.requireNonNull(ruleServiceInstantiationFactory, "ruleServiceInstantiationFactory cannot be null");
    }
}
