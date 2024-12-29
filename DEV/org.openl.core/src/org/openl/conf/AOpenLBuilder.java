package org.openl.conf;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.syntax.impl.Parser;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.SimpleVM;

public abstract class AOpenLBuilder extends BaseOpenLBuilder {

    protected SimpleVM createVM() {
        return new SimpleVM();
    }

    @Override
    public OpenL build() {
        try {

            IUserContext ucxt = getUserEnvironmentContext();

            String category = getCategory();
            IOpenLConfiguration conf = ucxt.getOpenLConfiguration(category);
            if (conf == null) {
                OpenLConfiguration oPconf = getOpenLConfiguration();
                String extendsCategory = getExtendsCategory();
                IOpenLConfiguration extendsConfiguration = null;
                if (extendsCategory != null) {
                    if ((extendsConfiguration = ucxt.getOpenLConfiguration(extendsCategory)) == null) {
                        throw new OpenLConfigurationException(
                                "The extended category " + extendsCategory + " must have been loaded first",
                                null);
                    }
                }

                IConfigurableResourceContext cxt = new ConfigurableResourceContext(ucxt.getUserClassLoader());

                oPconf.setParent(extendsConfiguration);
                oPconf.setConfigurationContext(cxt);
                oPconf.validate(cxt);

                ucxt.registerOpenLConfiguration(category, oPconf);
                conf = oPconf;
            }

            OpenL op = new OpenL();
            op.setParser(new Parser(conf));
            op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
            op.setVm(createVM());
            return op;
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    protected abstract OpenLConfiguration getOpenLConfiguration();

    protected abstract String getCategory();

    protected abstract String getExtendsCategory();
}
