package org.openl.extension;

import org.openl.*;
import org.openl.conf.BaseOpenLBuilder;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.OpenConfigurationException;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.xls.RulesCompileContext;

public abstract class ExtensionOpenLBuilder extends BaseOpenLBuilder {
    public OpenL build(String category) throws OpenConfigurationException {

        OpenL openl = new OpenL();

        openl.setParser(getParser());
        openl.setBinder(getBinder());
        openl.setVm(getOpenVM());
        openl.setCompileContext(getCompileContext());

        return openl;
    }

    protected abstract IOpenParser getParser();

    protected IOpenBinder getBinder() {
        return new XlsBinder(getUserEnvironmentContext());
    }

    protected IOpenVM getOpenVM() {
        return new SimpleRulesVM();
    }

    protected ICompileContext getCompileContext() {
        ICompileContext compileContext = new RulesCompileContext();

        IConfigurableResourceContext resourceContext = getResourceContext();

        if (resourceContext != null) {

            String propertyValue = resourceContext.findProperty("validation");

            if (propertyValue != null) {
                Boolean value = Boolean.valueOf(propertyValue);
                compileContext.setValidationEnabled(value);
            }
        }

        return compileContext;
    }
}
