package org.openl.xls;

import org.openl.OpenL;
import org.openl.conf.BaseOpenLBuilder;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.vm.SimpleRulesVM;

/**
 * @author snshor
 */
public class OpenLBuilder extends BaseOpenLBuilder {

    @Override
    public OpenL build() {

        OpenL openl = new OpenL();

        openl.setParser(new Parser(getUserEnvironmentContext()));
        openl.setBinder(new XlsBinder(new RulesCompileContext(), getUserEnvironmentContext()));
        openl.setVm(new SimpleRulesVM());

        return openl;
    }

}
