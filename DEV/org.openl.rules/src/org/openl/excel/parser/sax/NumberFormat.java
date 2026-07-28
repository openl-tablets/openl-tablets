package org.openl.excel.parser.sax;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class NumberFormat {
    @Getter
    private final int formatIndex;
    @Getter
    private final String formatString;
}
