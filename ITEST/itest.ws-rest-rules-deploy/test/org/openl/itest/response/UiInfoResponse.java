package org.openl.itest.response;

public class UiInfoResponse {

    private ServiceInfoResponse[] services;
    private Boolean deployerEnabled;

    public ServiceInfoResponse[] getServices() {
        return services;
    }

    public void setServices(ServiceInfoResponse[] services) {
        this.services = services;
    }

    public Boolean getDeployerEnabled() {
        return deployerEnabled;
    }

    public void setDeployerEnabled(Boolean deployerEnabled) {
        this.deployerEnabled = deployerEnabled;
    }
}
