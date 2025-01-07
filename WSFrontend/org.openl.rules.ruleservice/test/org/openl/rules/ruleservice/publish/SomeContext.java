package org.openl.rules.ruleservice.publish;

import org.openl.rules.annotations.ContextProperty;

public class SomeContext {

    private String lob;

    public void setLob(String lob) {
        this.lob = lob;
    }

    @ContextProperty("lob")
    public String getLob() {
        return lob;
    }
}
