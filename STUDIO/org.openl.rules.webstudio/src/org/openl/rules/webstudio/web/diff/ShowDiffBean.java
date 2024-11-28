package org.openl.rules.webstudio.web.diff;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@Service
@SessionScope
public class ShowDiffBean {
    private final DiffManager diffManager;

    public ShowDiffBean(DiffManager diffManager) {
        this.diffManager = diffManager;
    }

    public ShowDiffController getCurrent(String requestId) {
        if (StringUtils.isBlank(requestId)) {
            return null;
        }

        return diffManager.get(requestId);
    }

    public String getRequestId() {
        return WebStudioUtils.getRequestParameter("id");
    }
}
