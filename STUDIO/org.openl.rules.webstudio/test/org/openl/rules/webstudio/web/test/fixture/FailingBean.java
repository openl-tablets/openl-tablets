package org.openl.rules.webstudio.web.test.fixture;

public class FailingBean {
    public static FailingBean instance = new FailingBean();

    private FailingBean() {
        // some init logic for example and checking conditions
        throw new UnsupportedOperationException("Runtime environment should be correctly configured to use this class");
    }
}
