package org.openl.rules.webstudio.web.tableeditor;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

public class ShowMessageBean {

    public ShowMessageBean() {
    }

    public List<OpenLMessage> getMessage() {
        String type = FacesUtils.getRequestParameter("type");
        String summary = FacesUtils.getRequestParameter("summary");

        Severity severity;
        if (StringUtils.isNotBlank(type)) {
            severity = Severity.valueOf(type);
        } else {
            severity = Severity.INFO;
        }

        OpenLMessage message = new OpenLMessage(summary, StringUtils.EMPTY, severity);

        return Collections.singletonList(message);
    }

}
