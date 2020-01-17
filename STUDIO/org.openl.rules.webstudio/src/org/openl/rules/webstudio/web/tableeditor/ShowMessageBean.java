package org.openl.rules.webstudio.web.tableeditor;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ManagedBean
@RequestScoped
public class ShowMessageBean {

    public List<OpenLMessage> getMessage() {
        String type = FacesUtils.getRequestParameter("type");
        Integer openLMessageId = FacesUtils.getRequestIntParameter("summary", 0);

        Collection<OpenLMessage> moduleMessages = WebStudioUtils.getWebStudio().getModel().getModuleMessages();
        OpenLMessage openLMessage = moduleMessages.stream().filter(m -> m.getId() == openLMessageId).findFirst().get();

        Severity severity;
        if (StringUtils.isNotBlank(type)) {
            severity = Severity.valueOf(type);
        } else {
            severity = Severity.INFO;
        }

        OpenLMessage message = new OpenLMessage(openLMessage.getSummary(), severity);

        return Collections.singletonList(message);
    }

}
