package org.openl.rules.java.ce;

import org.openl.OpenL;
import org.openl.conf.AOpenLBuilder;
import org.openl.conf.NoAntOpenLTask;
import org.openl.conf.NodeBinderFactoryConfiguration;
import org.openl.conf.OpenConfigurationException;
import org.openl.conf.NodeBinderFactoryConfiguration.SingleBinderFactory;

public class OpenLBuilder extends AOpenLBuilder {

    @Override
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL.getInstance(OpenL.OPENL_JAVA_NAME, getUserEnvironmentContext());
        return super.build(category);
    }

    @Override
    public NoAntOpenLTask getNoAntOpenLTask() {
        NoAntOpenLTask op = new NoAntOpenLTask();

        op.setExtendsCategory(OpenL.OPENL_JAVA_NAME);
        op.setCategory(OpenL.OPENL_JAVA_CE_NAME);

        NodeBinderFactoryConfiguration nbc = op.createBindings();

        String[] binders = {
                "function", org.openl.binding.impl.ce.MethodNodeBinderMT.class.getName(),
        };

        for (int i = 0; i < binders.length / 2; i++) {
            SingleBinderFactory sbf = new SingleBinderFactory();
            sbf.setNode(binders[2 * i]);
            sbf.setClassName(binders[2 * i + 1]);
            nbc.addConfiguredBinder(sbf);

        }

        

        return op;
    }
}
