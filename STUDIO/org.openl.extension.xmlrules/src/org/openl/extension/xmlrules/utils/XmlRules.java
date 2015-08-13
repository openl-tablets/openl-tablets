package org.openl.extension.xmlrules.utils;

public class XmlRules {
    public static void Push(String cell, Object value) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }
        cache.push(cell, value);
    }

    public static void Pop(String cell) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }
        cache.pop(cell);
    }

    public static Object Cell(String cell) {
        LazyCellExecutor cache = LazyCellExecutor.getInstance();
        if (cache == null) {
            throw new IllegalStateException("Cells cache not initialized");
        }

        return cache.getCellValue(cell);
    }
}
