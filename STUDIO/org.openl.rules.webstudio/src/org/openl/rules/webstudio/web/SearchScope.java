package org.openl.rules.webstudio.web;

public enum SearchScope {

    CURRENT_MODULE("Current Module"),
    CURRENT_PROJECT("Current Project"),
    ALL_WITH_EXTRA_PROJECTS("ALL (includes dependency project)");

    private final String label;

    SearchScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
