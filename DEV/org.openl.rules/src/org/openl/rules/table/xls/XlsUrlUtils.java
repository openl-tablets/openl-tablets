/**
 * 
 */
package org.openl.rules.table.xls;

import org.openl.rules.table.IGridRegion;

/**
 * @author Andrei Astrouski
 */
public class XlsUrlUtils {

    public static boolean intersects(XlsUrlParser p1, String url2) {
        XlsUrlParser p2 = new XlsUrlParser();
        p2.parse(url2);

        if (!p1.wbPath.equals(p2.wbPath) || !p1.wbName.equals(p2.wbName) || !p1.wsName.equals(p2.wsName)) {
            return false;
        }

        if (p1.range == null || p2.range == null) {
            return false;
        }

        return IGridRegion.Tool.intersects(IGridRegion.Tool.makeRegion(p1.range), IGridRegion.Tool
                .makeRegion(p2.range));
    }
}
