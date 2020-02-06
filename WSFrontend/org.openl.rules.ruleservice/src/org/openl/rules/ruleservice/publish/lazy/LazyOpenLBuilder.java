package org.openl.rules.ruleservice.publish.lazy;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;

/**
 * A wrapper for replacing Java dynamic proxying.
 *
 * @author Yury Molchan
 * @see LazyOpenLConfigurator
 */
class LazyOpenLBuilder implements IOpenLBuilder {
    private final IOpenLBuilder builder;
    private final IUserContext ucxt;

    LazyOpenLBuilder(IOpenLBuilder builder, IUserContext ucxt) {
        this.builder = builder;
        this.ucxt = ucxt;
    }

    @Override
    public OpenL build(String category) {
        OpenL openl = builder.build(category);
        IOpenBinder newBinder = new LazyOpenLBinder(openl.getBinder(), ucxt);
        openl.setBinder(newBinder);
        return openl;
    }

    @Override
    public void setContexts(IConfigurableResourceContext resourceContext, IUserContext userEnvironmentContext) {
        builder.setContexts(resourceContext, userEnvironmentContext);
    }
}
