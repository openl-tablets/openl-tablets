/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.poi;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.util.Region;
import org.openl.IOpenSourceCodeModule;

/**
 * @author snshor
 *
 */
public class SheetModel {
    HSSFSheet sheet;
    IOpenSourceCodeModule xlsModule;

    int min_col = -1, max_col = -1, min_row = -1, max_row = -1;

    String name;

    int numMergedRegions = -1;

    static Object getCellValue(HSSFCell cell) {
        switch (cell.getCellType()) {
            case HSSFCell.CELL_TYPE_BLANK:
                return null;
            case HSSFCell.CELL_TYPE_BOOLEAN:
                return new Boolean(cell.getBooleanCellValue());
            case HSSFCell.CELL_TYPE_FORMULA:
            case HSSFCell.CELL_TYPE_NUMERIC:
                double value = cell.getNumericCellValue();
                return value == (int) value ? (Object) new Integer((int) value) : (Object) new Double(value);
            case HSSFCell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            default:
                return "unknown type: " + cell.getCellType();
        }
    }

    public static boolean isTopLeftOfTheMergedRegion(int x, int y, Region reg) {
        return reg.getColumnFrom() == x && reg.getColumnFrom() == y;
    }

    public SheetModel(HSSFSheet sheet, String name, IOpenSourceCodeModule xlsModule) {
        this.sheet = sheet;
        min_row = sheet.getFirstRowNum();
        max_row = sheet.getLastRowNum();
        this.name = name;
        this.xlsModule = xlsModule;
    }

    void calcBoundaries() {
        min_col = 10000;
        for (int i = min_row; i < max_row + 1; ++i) {
            HSSFRow row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            min_col = Math.min(min_col, row.getFirstCellNum());
            max_col = Math.max(max_col, row.getLastCellNum());
        }
    }

    public HSSFCell getCell(int x, int y) {
        HSSFRow row = sheet.getRow(y);
        if (row == null) {
            return null;
        }

        return row.getCell((short) x);

    }

    public int getMaxHeight() {
        return max_row - min_row + 1;
    }

    public int getMaxWidth() {
        if (min_col == -1) {
            calcBoundaries();
        }

        return max_col - min_col + 1;

    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    public int getNumMergedRegions() {
        if (numMergedRegions == -1) {
            try {
                numMergedRegions = sheet.getNumMergedRegions();
            } catch (NullPointerException e) {
                numMergedRegions = 0;
            }
        }
        return numMergedRegions;
    }

    public Region getRegionContaining(int x, int y) {
        int nregions = getNumMergedRegions();
        for (int i = 0; i < nregions; i++) {
            Region reg = sheet.getMergedRegionAt(i);
            if (reg.contains(y, (short) x)) {
                return reg;
            }
        }
        return null;
    }

    public String getStringValue(int x, int y) {
        HSSFCell cell = getCell(x, y);

        Object res = cell == null ? null : getCellValue(cell);

        return res == null ? null : String.valueOf(res);
    }

    public String getUri() {
        String xlsUri = xlsModule == null ? "" : xlsModule.getUri(0);
        return xlsUri + "#" + name;

    }

    public boolean isEmpty(int x, int y) {
        HSSFRow row = sheet.getRow(y);
        if (row == null) {
            return true;
        }

        HSSFCell cell = row.getCell((short) x);
        if (cell == null) {
            return true;
        }

        return cell.getCellType() == HSSFCell.CELL_TYPE_BLANK;

    }

    public boolean isPartOfMergedCell(int x, int y) {
        return getRegionContaining(x, y) != null;
    }

}
