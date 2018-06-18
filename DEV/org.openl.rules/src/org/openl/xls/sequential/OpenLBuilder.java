package org.openl.xls.sequential;

import org.openl.*;
import org.openl.conf.BaseOpenLBuilder;
import org.openl.conf.OpenConfigurationException;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.xls.RulesCompileContext;

public class OpenLBuilder extends org.openl.xls.OpenLBuilder {
    @Override
    protected IOpenParser createParser() {
        return new SequentialParser(getUserEnvironmentContext());
    }
}
