/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 *
 */
public class Configurator {

    private static final Logger LOG = LoggerFactory.getLogger(Configurator.class);

    public ClassLoader getClassLoader() {
        try {
            return Thread.currentThread().getContextClassLoader();
        } catch (Exception t) {
            LOG.debug("Error occurred on getting class loader: ", t);
            return Configurator.class.getClassLoader();
        }
    }

}
