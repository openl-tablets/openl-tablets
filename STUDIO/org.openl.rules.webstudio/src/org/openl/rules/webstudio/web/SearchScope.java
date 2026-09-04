package org.openl.rules.webstudio.web;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum SearchScope {

    CURRENT_MODULE("Current Module"),
    CURRENT_PROJECT("Current Project"),
    ALL("ALL (includes dependency projects)");

    @Getter
    private final String label;
}
