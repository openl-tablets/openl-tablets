package org.openl.xls;

import org.openl.OpenL;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.vm.SimpleRulesVM;

/**
 * @author snshor
 */
public class OpenLBuilder implements IOpenLBuilder {

    @Override
    public OpenL build(IUserContext userContext) {

        OpenL openl = new OpenL();

        openl.setParser(new Parser(userContext));
        openl.setBinder(new XlsBinder(new RulesCompileContext(), userContext));
        openl.setVm(new SimpleRulesVM());

        return openl;
    }

}
