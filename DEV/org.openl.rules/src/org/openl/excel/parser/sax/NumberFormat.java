package org.openl.excel.parser.sax;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class NumberFormat {
    private final int formatIndex;
    private final String formatString;

    public int getFormatIndex() {
        return formatIndex;
    }

    public String getFormatString() {
        return formatString;
    }
}
