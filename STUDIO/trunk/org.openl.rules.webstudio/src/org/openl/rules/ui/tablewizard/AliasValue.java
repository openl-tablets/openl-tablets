package org.openl.rules.ui.tablewizard;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;

/**
 * @author Andrei Astrouski
 */
public class AliasValue {

    @NotBlank(message="Can not be empty")
    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9]*)?", message="Invalid value")
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
