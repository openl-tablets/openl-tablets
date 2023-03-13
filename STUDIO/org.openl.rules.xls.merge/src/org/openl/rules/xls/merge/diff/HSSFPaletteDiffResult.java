package org.openl.rules.xls.merge.diff;

import java.util.Map;
import java.util.Set;

/**
 * Difference result of the same workbook by HSSF Palette between three revisions. {@code base revision} to
 * {@code our revision}, {@code base revision} to {@code their revision}
 * 
 * @author Vladyslav Pikus
 */
public class HSSFPaletteDiffResult {

    /**
     * Merge decision results for palette colors
     */
    private final Map<DiffStatus, Set<Short>> diffResults;
    /**
     * Colors matching results between {@code base revision} and {@code their revision}
     */
    private final Map<Short, XlsMatch> theirToBase;

    public HSSFPaletteDiffResult(Map<DiffStatus, Set<Short>> diffResults, Map<Short, XlsMatch> theirToBase) {
        this.diffResults = diffResults;
        this.theirToBase = theirToBase;
    }

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
     * Get matching result of requested color between {@code base revision} and {@code their revision}
     *
     * @param cInx color index
     * @return matching result
     */
    public XlsMatch getTheirMatchResult(Short cInx) {
        return theirToBase.get(cInx);
    }
}