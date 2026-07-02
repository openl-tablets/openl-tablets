package org.openl.rules.project.abstraction;

/**
 * Created by AAstrouski on 05.12.13.
 */
public enum ProjectStatus {

    LOCAL("Local"),
    DELETED("Deleted"),
    VIEWING("No Changes"),
    VIEWING_VERSION("Viewing Revision"),
    EDITING("In Editing"),
    CLOSED("Closed");

    private final String displayValue;

    ProjectStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

}
