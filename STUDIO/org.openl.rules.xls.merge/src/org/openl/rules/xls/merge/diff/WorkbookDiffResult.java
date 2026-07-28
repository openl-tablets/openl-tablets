package org.openl.rules.xls.merge.diff;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Difference result of the same workbook
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class WorkbookDiffResult {

    @Getter
    private final SheetDiffResult sheetDiffResult;
    @Getter
    private final HSSFPaletteDiffResult paletteDiffResult;

    public boolean hasConflicts() {
        return sheetDiffResult.hasConflicts() || paletteDiffResult.hasConflicts();
    }

    public boolean hasChangesToMerge() {
        return sheetDiffResult.hasChangesToMerge() || paletteDiffResult.hasChangesToMerge();
    }

}
