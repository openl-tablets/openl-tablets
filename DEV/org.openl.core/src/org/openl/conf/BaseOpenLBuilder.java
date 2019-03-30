package org.openl.conf;

public abstract class BaseOpenLBuilder implements IOpenLBuilder {

    private IConfigurableResourceContext resourceContext;
    private IUserContext userEnvironmentContext;

    public IConfigurableResourceContext getResourceContext() {
        return resourceContext;
    }

    public IUserContext getUserEnvironmentContext() {
        return userEnvironmentContext;
    }

    @Override
    public void setContexts(IConfigurableResourceContext resourceContext, IUserContext userEnvironmentContext) {

        this.resourceContext = resourceContext;
        this.userEnvironmentContext = userEnvironmentContext;
    }
}
