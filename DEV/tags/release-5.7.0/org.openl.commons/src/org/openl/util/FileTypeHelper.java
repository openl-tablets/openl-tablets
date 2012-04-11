package org.openl.util;

public class FileTypeHelper {
    public static boolean isExcelFile(String fileName) {
        String s = (fileName == null) ? "" : fileName.toLowerCase();
        if (s.endsWith(".xls")) return true;
        if (s.endsWith(".xlsx")) return true;
        return false;
    }

    public static boolean isWordFile(String fileName) {
        String s = (fileName == null) ? "" : fileName.toLowerCase();
        if (s.endsWith(".doc")) return true;
        if (s.endsWith(".docx")) return true;
        return false;
    }
}
