package org.openl.rules.webstudio.web.diff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@RequiredArgsConstructor
@Service
@SessionScope
public class ShowDiffBean {
    private final DiffManager diffManager;

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
