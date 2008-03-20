package org.openl.rules.common.config;

import java.io.InputStream;

/**
 * @author Aleh Bykhavets
 */
public class ClassPathConfigLocator extends ConfigLocator {
    public InputStream locate(String fullName) {
        return this.getClass().getClassLoader().getResourceAsStream(fullName);
    }
}
