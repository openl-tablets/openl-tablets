package org.openl.excel.parser.event.style;

import org.apache.poi.hssf.record.PaletteRecord;
import org.apache.poi.hssf.util.HSSFColor;

final class PoiUtils {
    private PoiUtils() {
    }

    public static short[] toRgb(PaletteRecord palette, short index) {
        // Handle the special AUTOMATIC case
        if (index == HSSFColor.HSSFColorPredefined.AUTOMATIC.getIndex()) {
            return HSSFColor.HSSFColorPredefined.AUTOMATIC.getColor().getTriplet();
        }
        byte[] b = palette.getColor(index);
        return (b == null) ? null : new short[] { (short) (b[0] & 0xff), (short) (b[1] & 0xff), (short) (b[2] & 0xff) };
    }
}
