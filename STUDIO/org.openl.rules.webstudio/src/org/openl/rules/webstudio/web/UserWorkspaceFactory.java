package org.openl.rules.webstudio.web;

import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class UserWorkspaceFactory extends AbstractFactoryBean<UserWorkspace> {

    public UserWorkspaceFactory() {
        setSingleton(false);
    }

    @Override
    public Class<?> getObjectType() {
        return UserWorkspace.class;
    }

    @Override
    protected UserWorkspace createInstance() throws Exception {
        return WebStudioUtils.getUserWorkspace(WebStudioUtils.getSession());
    }
}
