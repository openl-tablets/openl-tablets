package org.openl.excel.parser.sax;

public final class NumberFormat {
    private final int formatIndex;
    private final String formatString;

    NumberFormat(int formatIndex, String formatString) {
        this.formatIndex = formatIndex;
        this.formatString = formatString;
    }

    public int getFormatIndex() {
        return formatIndex;
    }

    public String getFormatString() {
        return formatString;
    }
}
