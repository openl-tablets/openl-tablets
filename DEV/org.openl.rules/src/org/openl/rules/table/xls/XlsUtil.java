package org.openl.rules.table.xls;

public final class XlsUtil {

    private XlsUtil() {
    }

    public static String xlsCellPresentation(int x, int y) {
        StringBuilder buf = new StringBuilder();
        int div = 'Z' - 'A' + 1;

        int xx = x;
        while (xx >= div) {
            int dd = xx / div;
            buf.append((char) ('A' + dd - 1));
            xx -= dd * div;
        }

        buf.append((char) ('A' + xx));

        buf.append(y + 1);
        return buf.toString();
    }

}
