package org.openl.rules.xls.merge.diff;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Difference result of the same workbook by sheet between three revisions. {@code base revision} to
 * {@code our revision}, {@code base revision} to {@code their revision}
 */
public class SheetDiffResult {

    /**
     * Merge decision results for sheets
     */
    private final Map<DiffStatus, Set<String>> diffResults;
    /**
     * Sheets matching results between {@code base revision} and {@code their revision}
     */
    private final Map<String, XlsMatch> theirToBase;

    public SheetDiffResult(Map<DiffStatus, Set<String>> diffResults, Map<String, XlsMatch> theirToBase) {
        this.diffResults = diffResults;
        this.theirToBase = theirToBase;
    }

    /**
     * Check ff any conflicted changes in sheets is detected between three revision
     * 
     * @return {@code true} if conflict is found, otherwise {@code false}
     */
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
        Set<String> diffResult = diffResults.get(diff);
        return diffResult != null && !diffResult.isEmpty();
    }

    /**
     * Get sheets for diff resolution
     *
     * @param diff diff
     * @return sheets collection
     */
    public Collection<String> getDiffSheets(DiffStatus diff) {
        return diffResults.getOrDefault(diff, Collections.emptySet());
    }

    /**
     * Get matching result of requested sheet between {@code base revision} and {@code their revision}
     * 
     * @param sheetName sheet name
     * @return matching result
     */
    public XlsMatch getTheirMatchResult(String sheetName) {
        return theirToBase.get(sheetName);
    }

}
