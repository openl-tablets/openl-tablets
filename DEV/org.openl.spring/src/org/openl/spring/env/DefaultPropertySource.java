package org.openl.spring.env;

/**
 * OpenL default property sources. Collects all openl-default.properties files.
 * 
 * Note: All openl-default.properties must contains unique keys.
 * 
 * @author Yury Molchan
 */
public class DefaultPropertySource extends CompositePropertySource {
    public static final String PROPS_NAME = "OpenL default properties";

    DefaultPropertySource() {
        super(PROPS_NAME);
        addLocation("classpath*:openl-default.properties");
    }
}
