package org.openl.util;

import org.apache.commons.lang.StringUtils;

public class FileTypeHelper {

    public static boolean isExcelFile(String fileName) {
        return StringUtils.endsWithIgnoreCase(fileName, ".xls")
            || StringUtils.endsWithIgnoreCase(fileName, ".xlsx");
    }

    public static boolean isWordFile(String fileName) {
        return StringUtils.endsWithIgnoreCase(fileName, ".doc")
        || StringUtils.endsWithIgnoreCase(fileName, ".docx");
    }

    public static boolean isZipFile(String fileName) {
        return StringUtils.endsWithIgnoreCase(fileName, ".zip");
    }

}
