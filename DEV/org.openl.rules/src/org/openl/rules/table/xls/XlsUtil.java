package org.openl.rules.table.xls;

public final class XlsUtil {

    private XlsUtil() {
    }

    public static String xlsCellPresentation(int x, int y) {
        var buf = new StringBuilder();
        var div = 'Z' - 'A' + 1;

        var xx = x;
        while (xx >= div) {
            var dd = xx / div;
            buf.append((char) ('A' + dd - 1));
            xx -= dd * div;
        }

        buf.append((char) ('A' + xx));

        buf.append(y + 1);
        return buf.toString();
    }

}
