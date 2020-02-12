package org.openl.rules.ruleservice.core;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.openl.OpenClassUtil;
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
        // Can happen when service was deployed unsuccessfully.
        if (sd == null || sd.getDeployment().getVersion().compareTo(serviceDescription.getDeployment().getVersion()) != 0) {
            Lock lock = RuleServiceRedeployLock.getInstance().getWriteLock();
            try {
                lock.lock();
                undeploy(service);
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
    public void undeploy(OpenLService service) throws RuleServiceUndeployException {
        Objects.requireNonNull(service, "service cannot be null");
        try {
            OpenLServiceHolder.getInstance().setOpenLService(service);
            ServiceDescription serviceDescription = serviceDescriptionMap.get(service.getName());
            try {
                ruleServiceManager.undeploy(service.getName());
                serviceDescriptionMap.remove(service.getName());
            } finally {
                // null can happen when service was deployed unsuccessfully.
                if (serviceDescription != null) {
                    cleanDeploymentResources(serviceDescription);
                }
                ClassLoader classloader = null;
                try {
                    classloader = service.getClassLoader();
                } catch (RuleServiceInstantiationException ignored) {
                }
                OpenClassUtil.releaseClassLoader(classloader);
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
                String.format("The service with name '%s' has already been deployed.", serviceDescription.getName()));
        }
        try {
            OpenLService newService = ruleServiceInstantiationFactory.createService(serviceDescription);
            OpenLServiceHolder.getInstance().setOpenLService(newService);
            ServiceDescription sd = serviceDescriptionMap.get(serviceDescription.getName());
            if (sd != null) {
                throw new IllegalStateException("This should not happen.");
            }
            serviceDescriptionMap.put(serviceDescription.getName(), serviceDescription);
            ruleServiceManager.deploy(newService);
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
        this.ruleServiceInstantiationFactory = Objects.requireNonNull(ruleServiceInstantiationFactory,
            "ruleServiceInstantiationFactory cannot be null");
    }
}
