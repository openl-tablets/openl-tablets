package org.openl.rules.webstudio.web;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import org.openl.rules.ui.ParameterRegistry;
import org.openl.rules.webstudio.web.jsf.WebContext;
import org.openl.rules.webstudio.web.tableeditor.TableBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

/**
 * Request scope managed bean providing logic for Main page.
 */
@Service
@RequestScope
@Slf4j
public class MainBean {

    private String requestId;

    public MainBean() {
        if (WebContext.getContextPath() == null) {
            WebContext.setContextPath(WebStudioUtils.getExternalContext().getRequestContextPath());
        }
        requestId = UUID.randomUUID().toString();
    }

    /**
     * Stub method that used for bean initialization.
     */
    public String getInit() {
        WebStudioUtils.getOrCreateWebStudio();
        return StringUtils.EMPTY;
    }

    public void reload() {
        WebStudioUtils.getWebStudio().resetProjects();
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void onPageUnload() {
        if (StringUtils.isNotEmpty(requestId)) {
            log.debug("Page unload for request id: {}", requestId);
            ParameterRegistry.remove(requestId);
            TableBean.tryUnlock(requestId);
        }
    }
}
