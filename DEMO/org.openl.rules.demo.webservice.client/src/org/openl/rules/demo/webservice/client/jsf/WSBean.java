package org.openl.rules.demo.webservice.client.jsf;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.openl.rules.demo.webservice.client.WSClient;

@ManagedBean
@RequestScoped
public class WSBean {

    @ManagedProperty("#{client}")
    WSClient service;

    public WSClient getService() {
        return service;
    }

    public void setService(WSClient service) {
        this.service = service;
    }

    private Object result;
    private String methodName;

    public Object getResult() {
        return result;
    }

    public String getMethodName() {
        return methodName;
    }

    public void invoke(String method) {
        invoke(method, new Object[0]);
    }

    public void invoke(String method, Object... params) {
        methodName = method;
        try {
            result = service.invoke(method, params);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }
}
