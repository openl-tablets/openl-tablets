package org.openl.rules.webstudio.web;

public enum SearchScope {

    CURRENT_MODULE("Current Module"),
    CURRENT_PROJECT("Current Project"),
    ALL("ALL (includes dependency projects)");

    private final String label;

    SearchScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
