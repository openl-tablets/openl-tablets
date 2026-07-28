package org.openl.rules.webstudio.web;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchScope {

    CURRENT_MODULE("Current Module"),
    CURRENT_PROJECT("Current Project"),
    ALL("ALL (includes dependency projects)");

    private final String label;

    public String getLabel() {
        return label;
    }
}
