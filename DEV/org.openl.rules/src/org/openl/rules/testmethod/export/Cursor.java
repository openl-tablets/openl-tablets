package org.openl.rules.testmethod.export;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class Cursor {
    @Getter
    private final int rowNum;
    @Getter
    private final int colNum;
}
