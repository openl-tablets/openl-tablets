package org.openl.rules.ui.tablewizard;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Andrei Astrouski
 */
public class AliasValue {

    @NotEmpty(message="Value can not be empty")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
