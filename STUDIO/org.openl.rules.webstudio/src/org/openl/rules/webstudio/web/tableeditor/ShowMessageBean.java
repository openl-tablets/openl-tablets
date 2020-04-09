package org.openl.rules.webstudio.web.tableeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

@Controller
@RequestScope
public class ShowMessageBean {

    public List<OpenLMessage> getMessage() {
        String type = WebStudioUtils.getRequestParameter("type");
        String value = WebStudioUtils.getRequestParameter("summary");
        final int openLMessageId;
        try {
            openLMessageId = Integer.parseInt(value);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

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
