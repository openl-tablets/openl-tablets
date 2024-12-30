package org.openl.j;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IOpenLConfiguration;
import org.openl.conf.IUserContext;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.OpenLConfiguration;
import org.openl.conf.TypeResolver;
import org.openl.rules.lang.xls.Parser;
import org.openl.rules.vm.SimpleRulesVM;

public class OpenLBuilder implements IOpenLBuilder {

    @Override
    public OpenL build(IUserContext ucxt) {
        IOpenLConfiguration conf = ucxt.getOpenLConfiguration(OpenL.OPENL_J_NAME);
        if (conf == null) {
            OpenLConfiguration oPconf = getOpenLConfiguration(ucxt.getUserClassLoader());

            ucxt.registerOpenLConfiguration(OpenL.OPENL_J_NAME, oPconf);
            conf = oPconf;
        }

        OpenL op = new OpenL();
        op.setParser(new Parser());
        op.setBinder(new Binder(conf, conf, conf, conf, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

    private static OpenLConfiguration getOpenLConfiguration(ClassLoader classLoader) {
        var op = new OpenLConfiguration();
        op.setMethodFactory(new LibrariesRegistry());
        op.setTypeResolver(new TypeResolver(classLoader));
        return op;
    }
}
