package org.openl.rules.webstudio.web.diff;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@ManagedBean
@SessionScoped
public class ShowDiffBean {
    @ManagedProperty(value = "#{diffManager}")
    private DiffManager diffManager;

    public ShowDiffController getCurrent(String requestId) {
        if (StringUtils.isBlank(requestId)) {
            return null;
        }

        return diffManager.get(requestId);
    }

    public void close(String requestId) {
        if (StringUtils.isNotBlank(requestId)) {
            diffManager.scheduleForRemove(requestId);
        }
    }

    public void setDiffManager(DiffManager diffManager) {
        this.diffManager = diffManager;
    }

    public String getRequestId() {
        return WebStudioUtils.getRequestParameter("id");
    }
}
