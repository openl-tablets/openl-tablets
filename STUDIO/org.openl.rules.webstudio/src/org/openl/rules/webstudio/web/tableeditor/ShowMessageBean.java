package org.openl.rules.webstudio.web.tableeditor;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.util.StringUtils;

@ManagedBean
@RequestScoped
public class ShowMessageBean {

    public List<OpenLMessage> getMessage() {
        String type = FacesUtils.getRequestParameter("type");
        String summary = FacesUtils.getRequestParameter("summary");

        Severity severity;
        if (StringUtils.isNotBlank(type)) {
            severity = Severity.valueOf(type);
        } else {
            severity = Severity.INFO;
        }

        OpenLMessage message = new OpenLMessage(summary, severity);

        return Collections.singletonList(message);
    }

}
