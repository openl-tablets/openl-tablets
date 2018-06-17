package org.openl.extension;

import org.openl.ICompileContext;
import org.openl.IOpenBinder;
import org.openl.IOpenParser;
import org.openl.IOpenVM;
import org.openl.OpenL;
import org.openl.conf.BaseOpenLBuilder;
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
        return new RulesCompileContext();
    }
}
