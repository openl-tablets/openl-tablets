package org.openl.excel.parser;

import org.apache.poi.openxml4j.util.ZipArchiveFakeEntry;
import org.apache.poi.openxml4j.util.ZipSecureFile;

public final class ExcelUtils {

    /**
     * The largest part of a workbook, such as the sheet of a large table, that is read into memory: 1 GB.
     *
     * <p>POI refuses a part over 100 MB by default, so a workbook with a large sheet could not be read for editing. A
     * larger part is not kept in a temporary file, because a workbook is never closed and the file would stay on disk.
     */
    static final int MAX_PART_BYTES = 1 << 30;

    public static void configureZipBombDetection() {
        // ZIP bomb detection tuning. Don't disable it by setting it in 0.
        // https://bz.apache.org/bugzilla/show_bug.cgi?id=58499
        // 0.001 is when 1MByte expands to 1 GByte
        ZipSecureFile.setMinInflateRatio(0.001);
        ZipArchiveFakeEntry.setMaxEntrySize(MAX_PART_BYTES);
    }

    private ExcelUtils() {
    }
}
