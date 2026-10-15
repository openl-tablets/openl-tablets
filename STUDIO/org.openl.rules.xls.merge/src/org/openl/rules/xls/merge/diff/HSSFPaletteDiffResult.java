package org.openl.rules.xls.merge.diff;

import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * Difference result of the same workbook by HSSF Palette between three revisions. {@code base revision} to
 * {@code our revision}, {@code base revision} to {@code their revision}
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class HSSFPaletteDiffResult {

    /**
     * Merge decision results for palette colors
     */
    private final Map<DiffStatus, Set<Short>> diffResults;

    public boolean hasConflicts() {
        return hasResults(DiffStatus.CONFLICT);
    }

    /**
     * Check if conflicts can be automatically resolved between {@code our revision} and {@code their revision}
     *
     * @return {@code true} if two revisions can be automatically merged, otherwise {@code false}
     */
    public boolean hasChangesToMerge() {
        return !hasConflicts() && hasResults(DiffStatus.THEIR);
    }

    private boolean hasResults(DiffStatus diff) {
        var diffResult = diffResults.get(diff);
        return diffResult != null && !diffResult.isEmpty();
    }

    /**
     * Tells whether the merge takes a color from {@code their revision}: their revision alone changed it, or a
     * resolution took the palette from their revision.
     *
     * @param cInx color index
     */
    public boolean isTakenFromTheirs(short cInx) {
        var theirs = diffResults.get(DiffStatus.THEIR);
        return theirs != null && theirs.contains(cInx);
    }
}
