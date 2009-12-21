package org.openl.conf;


public abstract class BaseOpenLBuilder implements IOpenLBuilder {

    private IConfigurableResourceContext resourceContext;
    private IUserEnvironmentContext userEnvironmentContext;

    public IConfigurableResourceContext getResourceContext() {
        return resourceContext;
    }

    public IUserEnvironmentContext getUserEnvironmentContext() {
        return userEnvironmentContext;
    }

    public void setContexts(IConfigurableResourceContext resourceContext, IUserEnvironmentContext userEnvironmentContext) {

        this.resourceContext = resourceContext;
        this.userEnvironmentContext = userEnvironmentContext;
    }
}
