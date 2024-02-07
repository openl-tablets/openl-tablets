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
    public OpenL build(String openl) {
        OpenL op = new OpenL();
        op.setName(openl);
        try {
            NoAntOpenLTask naot = getNoAntOpenLTask();

            naot.execute(getUserEnvironmentContext());

            IOpenLConfiguration conf = NoAntOpenLTask.retrieveConfiguration();

            op.setParser(new Parser(conf));

            op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
            op.setVm(createVM());
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return op;
    }

    public abstract NoAntOpenLTask getNoAntOpenLTask();
}
