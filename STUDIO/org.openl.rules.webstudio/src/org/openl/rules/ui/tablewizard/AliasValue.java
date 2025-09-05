package org.openl.rules.ui.tablewizard;


import jakarta.validation.constraints.NotBlank;

/**
 * @author Andrei Astrouski
 */
public class AliasValue {

    @NotBlank(message = "Cannot be empty")
    private String value;
    private String submittedValue;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSubmittedValue() {
        return submittedValue;
    }

    public void setSubmittedValue(String submittedValue) {
        this.submittedValue = submittedValue;
    }

}
