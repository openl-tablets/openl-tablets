package org.openl.rules.xls.merge.diff;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;

/**
 * Difference result of the same workbook by sheet between three revisions. {@code base revision} to
 * {@code our revision}, {@code base revision} to {@code their revision}
 */
@RequiredArgsConstructor
public class SheetDiffResult {

    /**
     * Merge decision results for sheets
     */
    private final Map<DiffStatus, Set<String>> diffResults;
    /**
     * Cell by cell merge plans of the sheets changed in both revisions
     */
    private final Map<String, SheetMergePlan> mergePlans;

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
        return !hasConflicts()
                && (hasResults(DiffStatus.THEIR) || hasResults(DiffStatus.MERGED) || hasResults(DiffStatus.BASE));
    }

    /**
     * Tells whether the merge takes a sheet, or a table of a sheet, from BASE revision, as a resolution may decide,
     * so BASE revision is read for the merge.
     */
    public boolean takesBase() {
        return hasResults(DiffStatus.BASE) || mergePlans.values().stream().anyMatch(SheetMergePlan::takesBase);
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
        return diffResults.getOrDefault(diff, Set.of());
    }

    /**
     * Get the cell by cell merge plans of the sheets changed in both revisions
     *
     * @return merge plans by the names of the sheets with {@link DiffStatus#MERGED} decision
     */
    public Map<String, SheetMergePlan> getMergePlans() {
        return mergePlans;
    }

}
