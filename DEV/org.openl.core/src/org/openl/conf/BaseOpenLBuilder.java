package org.openl.conf;

public abstract class BaseOpenLBuilder implements IOpenLBuilder {

    private IUserContext userEnvironmentContext;

    public IUserContext getUserEnvironmentContext() {
        return userEnvironmentContext;
    }

    @Override
    public void setContexts(IUserContext userEnvironmentContext) {

        this.userEnvironmentContext = userEnvironmentContext;
    }
}
