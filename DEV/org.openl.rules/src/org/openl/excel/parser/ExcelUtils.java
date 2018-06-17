package org.openl.excel.parser;

import org.apache.poi.openxml4j.util.ZipSecureFile;

public final class ExcelUtils {
    public static void configureZipBombDetection() {
        // ZIP bomb detection tuning. Don't disable it by setting it in 0.
        // https://bz.apache.org/bugzilla/show_bug.cgi?id=58499
        // 0.001 is when 1MByte expands to 1 GByte
        ZipSecureFile.setMinInflateRatio(0.001);
    }

    private ExcelUtils() {
    }
}
