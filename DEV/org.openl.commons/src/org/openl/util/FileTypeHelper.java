package org.openl.util;

import org.apache.commons.lang3.StringUtils;

public class FileTypeHelper {

    public static boolean isExcelFile(String fileName) {
        return StringUtils.endsWithIgnoreCase(fileName, ".xls") || StringUtils.endsWithIgnoreCase(fileName, ".xlsx")
                || StringUtils.endsWithIgnoreCase(fileName, ".xlsm");
    }

    public static boolean isZipFile(String fileName) {
        return StringUtils.endsWithIgnoreCase(fileName, ".zip");
    }

}
