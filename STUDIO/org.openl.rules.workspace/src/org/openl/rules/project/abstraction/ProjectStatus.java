package org.openl.rules.project.abstraction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by AAstrouski on 05.12.13.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public enum ProjectStatus {

    LOCAL("Local"),
    DELETED("Deleted"),
    VIEWING("No Changes"),
    VIEWING_VERSION("Viewing Revision"),
    EDITING("In Editing"),
    CLOSED("Closed");

    @Getter
    private final String displayValue;

}
