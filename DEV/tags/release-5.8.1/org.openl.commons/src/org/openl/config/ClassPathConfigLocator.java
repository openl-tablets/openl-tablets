package org.openl.config;

import java.io.InputStream;

/**
 * @author Aleh Bykhavets
 */
public class ClassPathConfigLocator extends ConfigLocator {
    @Override
    public InputStream locate(String fullName) {
        return this.getClass().getClassLoader().getResourceAsStream(fullName);
    }
}
