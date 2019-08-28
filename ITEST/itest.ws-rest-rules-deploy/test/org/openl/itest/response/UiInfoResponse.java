package org.openl.itest.response;

public class UiInfoResponse {

    private ServiceInfoResponse[] serviceInfo = new ServiceInfoResponse[0];

    public ServiceInfoResponse[] getServiceInfo() {
        return serviceInfo;
    }

    public void setServiceInfo(ServiceInfoResponse[] serviceInfo) {
        this.serviceInfo = serviceInfo;
    }
}
