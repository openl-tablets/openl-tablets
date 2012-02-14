package org.openl.rules.ui.tablewizard;

import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Andrei Astrouski
 */
public class AliasValue {

    @NotBlank(message="Can not be empty")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
