package org.openl.rules.xls.merge.diff;

import lombok.RequiredArgsConstructor;

/**
 * Difference result of the same workbook
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class WorkbookDiffResult {

    private final SheetDiffResult sheetDiffResult;
    private final HSSFPaletteDiffResult paletteDiffResult;

    public boolean hasConflicts() {
        return sheetDiffResult.hasConflicts() || paletteDiffResult.hasConflicts();
    }

    public boolean hasChangesToMerge() {
        return sheetDiffResult.hasChangesToMerge() || paletteDiffResult.hasChangesToMerge();
    }

    public SheetDiffResult getSheetDiffResult() {
        return sheetDiffResult;
    }

    public HSSFPaletteDiffResult getPaletteDiffResult() {
        return paletteDiffResult;
    }

}
