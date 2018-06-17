package org.openl.excel.parser.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.record.*;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;

public class StyleTrackingListener implements HSSFListener {
    private final HSSFListener delegate;
    private final Map<Integer, FormatRecord> customFormats = new HashMap<>();
    private final List<ExtendedFormatRecord> extendedFormats = new ArrayList<>();
    private final List<FontRecord> fonts = new ArrayList<>();

    StyleTrackingListener(HSSFListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void processRecord(Record record) {
        if (record instanceof FormatRecord) {
            FormatRecord fr = (FormatRecord) record;
            customFormats.put(fr.getIndexCode(), fr);
        }
        if (record instanceof ExtendedFormatRecord) {
            ExtendedFormatRecord xr = (ExtendedFormatRecord) record;
            extendedFormats.add(xr);
        }
        if (record instanceof FontRecord) {
            fonts.add((FontRecord) record);
        }

        delegate.processRecord(record);
    }

    public String getFormatString(int formatIndex) {
        if (formatIndex < 0) {
            return null;
        }

        String format;
        if (formatIndex >= HSSFDataFormat.getNumberOfBuiltinBuiltinFormats() || customFormats.get(formatIndex) != null) {
            format = customFormats.get(formatIndex).getFormatString();
        } else {
            format = HSSFDataFormat.getBuiltinFormat((short) formatIndex);
        }
        return format;
    }

    /**
     * Returns the index of the format string, used by your cell, or -1 if none found
     *
     * @param cell the cell
     *
     * @return the index of the format string
     */
    public int getFormatIndex(CellValueRecordInterface cell) {
        ExtendedFormatRecord xfr = extendedFormats.get(cell.getXFIndex());
        if (xfr == null) {
            return -1;
        }
        return xfr.getFormatIndex();
    }

    public short getIndent(CellValueRecordInterface cell) {
        ExtendedFormatRecord xfr = extendedFormats.get(cell.getXFIndex());
        return xfr == null ? 0 : xfr.getIndent();
    }

    List<ExtendedFormatRecord> getExtendedFormats() {
        return extendedFormats;
    }

    Map<Integer,FormatRecord> getCustomFormats() {
        return customFormats;
    }

    List<FontRecord> getFonts() {
        return fonts;
    }
}
