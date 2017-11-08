package org.openl.rules.demo.webservice.client.jsf;

import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.openl.rules.demo.webservice.client.WSClient;

@ManagedBean
@RequestScoped
public class WSBean {

    public Map<String, UIInput> map = new HashMap<>(3);

    public Map<String, UIInput> getMap() {
        return map;
    }

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
        invoke(method, null);
    }

    public void invoke(String method, Map<String, UIInput> params) {
        methodName = method;
        try {
            String json = getJson(params);
            result = service.invoke(method, json);
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, e.getMessage(), null));
        }
    }

    private String getJson(Map<String, UIInput> params) {
        if (params == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(64);
        builder.append("{");
        boolean comma = false;
        for (Map.Entry<String, UIInput> entry : params.entrySet()) {
            if (comma) {
                builder.append(',');
            }
            comma = true;
            builder.append('"');
            builder.append(entry.getKey());
            builder.append("\":");
            Object value = entry.getValue().getValue();
            if (value instanceof String) {
                builder.append('"').append(value).append('"');
            } else {
                builder.append(value);
            }
        }
        builder.append('}');
        return builder.toString();
    }
}
