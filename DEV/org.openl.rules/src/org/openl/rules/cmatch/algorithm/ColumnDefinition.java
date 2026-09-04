package org.openl.rules.cmatch.algorithm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ColumnDefinition {
    @Getter
    private final String name;
    @Getter
    private final boolean isMultipleValueAllowed;
}
