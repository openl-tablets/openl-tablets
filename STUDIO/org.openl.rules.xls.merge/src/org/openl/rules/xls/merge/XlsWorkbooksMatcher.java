package org.openl.rules.xls.merge;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.xls.merge.diff.XlsMatch;

/**
 * This service prepares matching results of two workbooks by sheet
 */
public final class XlsWorkbooksMatcher {

    private XlsWorkbooksMatcher() {
    }

    /**
     * Match sheets from two workbooks
     *
     * @param baseWorkbook fist workbook
     * @param workbook second workbook
     * @return matching result
     */
    public static Map<String, XlsMatch> match(Workbook baseWorkbook, Workbook workbook) {
        Map<String, XlsMatch> sheetChangedResults = new HashMap<>();
        for (Sheet baseSheet : baseWorkbook) {
            Sheet sheet = workbook.getSheet(baseSheet.getSheetName());
            if (sheet == null) {
                sheetChangedResults.put(baseSheet.getSheetName(), XlsMatch.REMOVED);
            } else {
                boolean changed = XlsSheetsMatcher.hasChanges(baseWorkbook, baseSheet, workbook, sheet);
                sheetChangedResults.put(baseSheet.getSheetName(), changed ? XlsMatch.UPDATED : XlsMatch.EQUAL);
            }
        }
        for (Sheet sheet : workbook) {
            if (!sheetChangedResults.containsKey(sheet.getSheetName())) {
                sheetChangedResults.put(sheet.getSheetName(), XlsMatch.CREATED);
            }
        }
        return sheetChangedResults;
    }

}
