package org.openl.studio.projects.service.tables.format;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * Removes the cell formats and the named styles a workbook no longer uses.
 *
 * <p>A cell format is used when a cell, a row or a column of a sheet names it. The first cell format is the one every
 * cell without a format of its own takes, and stays. A named style, and the cell style it names, is used when a used
 * cell format links it; the Normal style, which the first cell format links, stays. Every cell, row, column and cell
 * format keeps the format or the style it had, renumbered.
 *
 * <p>A workbook that names no style is left without the list of named styles: an empty list is content a spreadsheet
 * application reads as damaged.
 *
 * <p>POI keeps the lists of the cell formats and of the cell styles to itself, so they are read from its fields.
 *
 * @author Yury Molchan
 */
final class UnusedStyles {

    private UnusedStyles() {
    }

    /**
     * Removes the cell formats no cell, row or column uses, then the named styles no cell format links.
     *
     * @param workbook the workbook
     */
    static void remove(XSSFWorkbook workbook) {
        var styles = workbook.getStylesSource();
        var cellXfs = listOf(styles, "xfs");
        var styleXfs = listOf(styles, "styleXfs");
        removeCellXfs(workbook, cellXfs);
        removeStyleXfs(styles, cellXfs, styleXfs);
    }

    @SuppressWarnings("unchecked")
    private static List<CTXf> listOf(StylesTable styles, String field) {
        try {
            return (List<CTXf>) FieldUtils.readDeclaredField(styles, field, true);
        } catch (IllegalAccessException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    /** Removes the cell formats no cell, row or column of a sheet names, and renumbers the ones they name. */
    private static void removeCellXfs(XSSFWorkbook workbook, List<CTXf> cellXfs) {
        var used = new boolean[cellXfs.size()];
        if (used.length == 0) {
            return;
        }
        used[0] = true;
        for (var sheet = 0; sheet < workbook.getNumberOfSheets(); sheet++) {
            markCellXfs(workbook.getSheetAt(sheet).getCTWorksheet(), used);
        }
        var renumbered = renumbering(used);
        for (var sheet = 0; sheet < workbook.getNumberOfSheets(); sheet++) {
            renumberCellXfs(workbook.getSheetAt(sheet).getCTWorksheet(), renumbered);
        }
        keep(cellXfs, used);
    }

    private static void markCellXfs(CTWorksheet worksheet, boolean[] used) {
        for (var cols : worksheet.getColsList()) {
            for (var col : cols.getColList()) {
                if (col.isSetStyle()) {
                    mark(used, col.getStyle());
                }
            }
        }
        for (var row : rowsOf(worksheet)) {
            if (row.isSetS()) {
                mark(used, row.getS());
            }
            for (var cell : row.getCList()) {
                if (cell.isSetS()) {
                    mark(used, cell.getS());
                }
            }
        }
    }

    private static void renumberCellXfs(CTWorksheet worksheet, int[] renumbered) {
        for (var cols : worksheet.getColsList()) {
            for (var col : cols.getColList()) {
                if (col.isSetStyle()) {
                    col.setStyle(renumber(renumbered, col.getStyle()));
                }
            }
        }
        for (var row : rowsOf(worksheet)) {
            if (row.isSetS()) {
                row.setS(renumber(renumbered, row.getS()));
            }
            for (var cell : row.getCList()) {
                if (cell.isSetS()) {
                    cell.setS(renumber(renumbered, cell.getS()));
                }
            }
        }
    }

    private static List<CTRow> rowsOf(CTWorksheet worksheet) {
        var data = worksheet.getSheetData();
        return data == null ? List.of() : data.getRowList();
    }

    /**
     * Removes the cell styles no cell format links, with the named styles naming them, and renumbers the links. A
     * workbook naming no style any more loses the list of named styles.
     */
    private static void removeStyleXfs(StylesTable styles, List<CTXf> cellXfs, List<CTXf> styleXfs) {
        var used = new boolean[styleXfs.size()];
        if (used.length == 0) {
            return;
        }
        // The Normal style is the first cell style of every workbook.
        used[0] = true;
        for (var xf : cellXfs) {
            mark(used, xf.getXfId());
        }
        var renumbered = renumbering(used);
        for (var xf : cellXfs) {
            xf.setXfId(renumber(renumbered, xf.getXfId()));
        }
        keep(styleXfs, used);
        var sheet = styles.getCTStylesheet();
        if (!sheet.isSetCellStyles()) {
            return;
        }
        var named = sheet.getCellStyles();
        var kept = new ArrayList<CTCellStyle>();
        for (var style : named.getCellStyleList()) {
            var at = style.getXfId();
            if (at < used.length && used[(int) at]) {
                style.setXfId(renumbered[(int) at]);
                kept.add((CTCellStyle) style.copy());
            }
        }
        if (kept.isEmpty()) {
            sheet.unsetCellStyles();
        } else {
            named.setCellStyleArray(kept.toArray(new CTCellStyle[0]));
            named.setCount(kept.size());
        }
    }

    private static void mark(boolean[] used, long index) {
        if (index >= 0 && index < used.length) {
            used[(int) index] = true;
        }
    }

    /** The new number of each entry kept, the entries removed numbered as the first one. */
    private static int[] renumbering(boolean[] used) {
        var renumbered = new int[used.length];
        var next = 0;
        for (var index = 0; index < used.length; index++) {
            renumbered[index] = used[index] ? next++ : 0;
        }
        return renumbered;
    }

    private static long renumber(int[] renumbered, long index) {
        return index >= 0 && index < renumbered.length ? renumbered[(int) index] : 0;
    }

    /**
     * Keeps the entries in use, each a copy of its own. POI writes the list into the stylesheet entry by entry, and the
     * entries a shorter list leaves over are dropped from it: an entry still read from the list, such as the cell
     * format of a cell when its sheet is written, must not be one of them.
     */
    private static void keep(List<CTXf> entries, boolean[] used) {
        var kept = new ArrayList<CTXf>(entries.size());
        for (var index = 0; index < entries.size(); index++) {
            if (used[index]) {
                kept.add((CTXf) entries.get(index).copy());
            }
        }
        entries.clear();
        entries.addAll(kept);
    }
}
