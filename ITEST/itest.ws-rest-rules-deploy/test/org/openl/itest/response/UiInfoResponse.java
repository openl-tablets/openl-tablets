package org.openl.itest.response;

public class UiInfoResponse {

    private ServiceInfoResponse[] services = new ServiceInfoResponse[0];

    public ServiceInfoResponse[] getServices() {
        return services;
    }

    public void setServices(ServiceInfoResponse[] services) {
        this.services = services;
    }
}
