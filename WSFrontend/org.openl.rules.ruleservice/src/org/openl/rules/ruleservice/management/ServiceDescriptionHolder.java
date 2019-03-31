package org.openl.rules.ruleservice.management;

import org.openl.rules.ruleservice.core.ServiceDescription;

public final class ServiceDescriptionHolder {
    private static final ServiceDescriptionHolder INSTANCE = new ServiceDescriptionHolder();

    private ThreadLocal<ServiceDescription> serviceDescriptionHolder = new ThreadLocal<>();

    private ServiceDescriptionHolder() {
    }

    public static ServiceDescriptionHolder getInstance() {
        return INSTANCE;
    }

    public ServiceDescription getServiceDescription() {
        return serviceDescriptionHolder.get();
    }

    public void setServiceDescription(ServiceDescription serviceDescription) {
        serviceDescriptionHolder.set(serviceDescription);
    }

    public void remove() {
        serviceDescriptionHolder.remove();
    }

}
