package org.openl.rules.webstudio.web.tableeditor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

@Service
@RequestScope
public class ShowMessageBean {

    public boolean isMessageOutdated() {
        return getMessage().isEmpty();
    }

    public List<OpenLMessage> getMessage() {
        String type = WebStudioUtils.getRequestParameter("type");
        String value = WebStudioUtils.getRequestParameter("summary");

        Severity severity;
        if (StringUtils.isNotBlank(type)) {
            severity = Severity.valueOf(type);
        } else {
            severity = Severity.INFO;
        }

        final int openLMessageId;
        try {
            openLMessageId = Integer.parseInt(value);
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        Collection<OpenLMessage> moduleMessages = WebStudioUtils.getWebStudio().getModel().getModuleMessages();
        List<OpenLMessage> openLMessage = moduleMessages.stream()
            .filter(m -> m.getId() == openLMessageId)
            .findFirst()
            .map(x -> new OpenLMessage(x.getSummary(), severity))
            .map(Collections::singletonList)
            .orElse(Collections.emptyList());

        return openLMessage;
    }

}
