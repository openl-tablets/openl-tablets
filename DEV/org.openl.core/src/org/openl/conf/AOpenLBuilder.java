package org.openl.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.syntax.impl.Parser;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.SimpleVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AOpenLBuilder extends BaseOpenLBuilder {

    private final Logger log = LoggerFactory.getLogger(AOpenLBuilder.class);

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

    protected Properties getProperties(String openl) {
        URL url = getResourceContext().findClassPathResource(openl.replace('.', '/') + '/' + openl + ".ant.properties");
        if (url == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = url.openStream();
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception t) {
                log.error("Failed to close an input stream.", t);
            }
        }

    }
}
