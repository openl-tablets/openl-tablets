package org.openl.rules.ruleservice.publish.lazy;

import org.openl.OpenL;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfigurator;

/**
 * Allows using {@link org.openl.rules.lang.xls.prebind.XlsPreBinder XlsPreBinder} for dependent modules on
 * LazyMultiModule projects. Creates an IOpenBinder proxy that uses XlsPreBinder on prebind step and XlsBinder on
 * compile step.
 *
 * @author NSamatov
 * @see LazyBinderInvocationHandler
 */
public class LazyOpenLConfigurator extends OpenLConfigurator {
    @Override
    public synchronized IOpenLBuilder getBuilder(String openlName, IUserContext ucxt) {
        IOpenLBuilder builder = super.getBuilder(openlName, ucxt);

        if (!openlName.startsWith(OpenL.OPENL_JAVA_RULE_NAME)) {
            return builder;
        }

        return new LazyOpenLBuilder(builder, ucxt);
    }

}
