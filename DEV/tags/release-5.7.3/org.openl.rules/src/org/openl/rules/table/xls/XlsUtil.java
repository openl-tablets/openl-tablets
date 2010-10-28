package org.openl.rules.table.xls;

public final class XlsUtil {

    public static String xlsCellPresentation(int x, int y) {
        StringBuffer buf = new StringBuffer();
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
