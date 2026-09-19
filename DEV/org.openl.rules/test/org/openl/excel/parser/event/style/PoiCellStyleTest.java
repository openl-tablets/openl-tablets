package org.openl.excel.parser.event.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.apache.poi.hssf.record.ExtendedFormatRecord;
import org.apache.poi.hssf.record.FormatRecord;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.junit.jupiter.api.Test;

class PoiCellStyleTest {

    /** A format index the workbook defined itself, past the ones Excel knows by heart. */
    private static final short CUSTOM_INDEX = 200;

    /** One of the formats Excel knows by heart: a percentage. */
    private static final short BUILTIN_INDEX = 9;

    @Test
    void readsACustomFormatFromTheFormatsOfTheWorkbook() {
        var style = styleFor(CUSTOM_INDEX, Map.of((int) CUSTOM_INDEX, new FormatRecord(CUSTOM_INDEX, "0.00%")));

        assertEquals("0.00%", style.getDataFormatString());
    }

    @Test
    void readsAFormatExcelKnowsByHeartWhenTheWorkbookDefinedNone() {
        var style = styleFor(BUILTIN_INDEX, Map.of());

        assertEquals(HSSFDataFormat.getBuiltinFormat(BUILTIN_INDEX), style.getDataFormatString());
    }

    @Test
    void answersWithNothingForAFormatThatIsNowhere() {
        var style = styleFor(CUSTOM_INDEX, Map.of());

        assertNull(style.getDataFormatString());
    }

    @Test
    void answersWithNothingWhenTheCellHasNoFormat() {
        var style = styleFor((short) -1, Map.of());

        assertNull(style.getDataFormatString());
    }

    private static PoiCellStyle styleFor(short formatIndex, Map<Integer, FormatRecord> formats) {
        var format = new ExtendedFormatRecord();
        format.setFormatIndex(formatIndex);
        return new PoiCellStyle((short) 1, format, formats);
    }
}
