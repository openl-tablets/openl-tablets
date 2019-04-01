/**
 *
 */
package org.openl.rules.table.xls;

import org.openl.rules.table.IGridRegion;

/**
 * @author Andrei Astrouski
 */
public class XlsUrlUtils {

    public static boolean intersects(XlsUrlParser p1, XlsUrlParser p2) {
        if (!p1.getWbPath().equals(p2.getWbPath()) || !p1.getWbName().equals(p2.getWbName()) || !p1.getWsName()
            .equals(p2.getWsName())) {
            return false;
        }

        if (p1.getRange() == null || p2.getRange() == null) {
            return false;
        }

        return IGridRegion.Tool.intersects(IGridRegion.Tool.makeRegion(p1.getRange()),
            IGridRegion.Tool.makeRegion(p2.getRange()));
    }
}
