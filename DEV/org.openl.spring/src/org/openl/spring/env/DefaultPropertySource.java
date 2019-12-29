package org.openl.spring.env;

public class DefaultPropertySource extends CompositePropertySource {
    public static final String PROPS_NAME = "OpenL default properties";

    DefaultPropertySource() {
        super(PROPS_NAME);
        addLocation("classpath*:openl-default.properties");
    }
}
