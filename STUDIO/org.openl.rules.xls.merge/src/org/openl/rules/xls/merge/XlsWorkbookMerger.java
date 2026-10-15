package org.openl.rules.xls.merge;

import static org.openl.rules.xls.merge.HSSFPaletteMatcher.FIRST_COLOR_INDEX;
import static org.openl.rules.xls.merge.HSSFPaletteMatcher.LAST_COLOR_INDEX;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFOptimiser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.HSSFPaletteDiffResult;
import org.openl.rules.xls.merge.diff.Resolution;
import org.openl.rules.xls.merge.diff.SheetConflictReason;
import org.openl.rules.xls.merge.diff.SheetConflicts;
import org.openl.rules.xls.merge.diff.SheetDiffResult;
import org.openl.rules.xls.merge.diff.SheetMergePlan;
import org.openl.rules.xls.merge.diff.TableConflict;
import org.openl.rules.xls.merge.diff.UnresolvedConflictsException;
import org.openl.rules.xls.merge.diff.WorkbookConflicts;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.rules.xls.merge.diff.XlsMatch;
import org.openl.util.IOUtils;

/**
 * This services helps to merge two conflicted revisions based on base revision
 *
 * @author Vladyslav Pikus
 */
@Slf4j
public class XlsWorkbookMerger implements Closeable {


    private final StreamWorkbook baseWorkbook;
    private final StreamWorkbook ourWorkbook;
    private final StreamWorkbook theirWorkbook;
    private final boolean hssf;

    private XlsWorkbookMerger(StreamWorkbook baseWorkbook, StreamWorkbook ourWorkbook, StreamWorkbook theirWorkbook) {
        this.baseWorkbook = baseWorkbook;
        this.ourWorkbook = ourWorkbook;
        this.theirWorkbook = theirWorkbook;
        this.hssf = baseWorkbook.unwrap() instanceof HSSFWorkbook;
    }

    /**
     * Close all workbooks
     *
     * @throws IOException if happen while closing
     */
    @Override
    public void close() throws IOException {
        IOException e = null;
        try {
            baseWorkbook.close();
        } catch (IOException e1) {
            e = e1;
        }
        try {
            ourWorkbook.close();
        } catch (IOException e1) {
            if (e != null) {
                e1.addSuppressed(e);
            }
            e = e1;
        }
        try {
            theirWorkbook.close();
        } catch (IOException e1) {
            if (e != null) {
                e1.addSuppressed(e);
            }
            e = e1;
        }
        if (e != null) {
            throw e;
        }
    }

    /**
     * Get difference result between three revisions by sheet
     *
     * @return difference result
     */
    public WorkbookDiffResult getDiffResult() {
        return diff(Conflicts.strict(), null);
    }

    /**
     * Compares the three revisions and lists the sheets either revision changed: how the merge takes each of them by
     * itself, and what of it both revisions changed in ways the merge cannot combine.
     *
     * <p>A conflicting sheet is resolved table by table, or only as a whole when its conflict lies outside its tables,
     * such as a sheet one revision removed and the other one changed.
     *
     * @return the sheets either revision changed, with their conflicts
     */
    public WorkbookConflicts getConflicts() {
        var sheets = new ArrayList<SheetConflicts>();
        var diff = diff(Conflicts.survey(), sheets);
        sheets.sort(Comparator.comparingInt(sheet -> sheetOrder(sheet.name())));
        return new WorkbookConflicts(List.copyOf(sheets), diff.getPaletteDiffResult().hasConflicts());
    }

    /**
     * Compares the three revisions and resolves their conflicts as decided.
     *
     * <p>A sheet decided as a whole is taken whole from the revision decided, its tables included. Everything both
     * revisions did not change in ways that conflict is merged as {@link #getDiffResult()} merges it.
     *
     * @param decisions how each conflict is resolved, by its identifier: the name of a sheet, the identifier of a table
     *                  conflict, or {@link WorkbookConflicts#PALETTE_ID}
     * @return the difference, with every conflict resolved
     * @throws UnresolvedConflictsException when a conflict has no decision, or the decisions leave a sheet a conflict,
     *                                      such as tables of the two revisions touching each other
     */
    public WorkbookDiffResult resolve(Map<String, Resolution> decisions) throws UnresolvedConflictsException {
        var conflicts = Conflicts.resolution(decisions);
        var diff = diff(conflicts, null);
        conflicts.requireResolved();
        return diff;
    }

    /**
     * A revision of the workbook, to read what a conflict holds there. It is not changed.
     */
    public Workbook revisionOf(Resolution revision) {
        return switch (revision) {
            case OURS -> ourWorkbook.unwrap();
            case THEIRS -> theirWorkbook.unwrap();
            case BASE -> baseWorkbook.unwrap();
        };
    }

    /**
     * Compares the three revisions sheet by sheet.
     *
     * @param survey the list the sheets either revision changed are added to, or {@code null} to list none
     */
    private WorkbookDiffResult diff(Conflicts conflicts, @Nullable List<SheetConflicts> survey) {
        var diffResult = new EnumMap<DiffStatus, Set<String>>(DiffStatus.class);
        Map<String, XlsMatch> ourToBase = XlsWorkbooksMatcher.match(baseWorkbook, ourWorkbook);
        Map<String, XlsMatch> theirToBase = XlsWorkbooksMatcher.match(baseWorkbook, theirWorkbook);

        var mergePlans = new TreeMap<String, SheetMergePlan>(String.CASE_INSENSITIVE_ORDER);

        final Function<DiffStatus, Set<String>> initGroupValue = key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (var entry : ourToBase.entrySet()) {
            var sheetName = entry.getKey();
            conflicts.startSheet();
            var diffDecision = decideSheet(sheetName, entry.getValue(), theirToBase, mergePlans, conflicts);
            if (diffDecision != null) {
                log.debug("{} resolution is chosen for '{}' sheet", diffDecision.name(), sheetName);
                diffResult.computeIfAbsent(diffDecision, initGroupValue).add(sheetName);
                if (survey != null) {
                    survey.add(surveyed(sheetName, diffDecision, conflicts));
                }
            }
        }
        for (String sheetName : theirToBase.keySet()) {
            if (!ourToBase.containsKey(sheetName)) {
                diffResult.computeIfAbsent(DiffStatus.THEIR, initGroupValue).add(sheetName);
                if (survey != null) {
                    conflicts.startSheet();
                    survey.add(surveyed(sheetName, DiffStatus.THEIR, conflicts));
                }
            }
        }

        var paletteDiff = calcPaletteDiff(conflicts);
        return new WorkbookDiffResult(new SheetDiffResult(diffResult, mergePlans), paletteDiff);
    }

    /**
     * Decides how to merge a sheet OUR revision has, or had in BASE revision.
     *
     * @return how the sheet is merged, or {@code null} when nothing is merged: both revisions left the sheet as it
     * was, removed it, or changed it alike
     */
    private @Nullable DiffStatus decideSheet(String sheetName,
                                             XlsMatch ourMatch,
                                             Map<String, XlsMatch> theirToBase,
                                             Map<String, SheetMergePlan> mergePlans,
                                             Conflicts conflicts) {
        var theirMatch = theirToBase.get(sheetName);
        if (ourMatch == theirMatch) {
            if (ourMatch == XlsMatch.EQUAL || ourMatch == XlsMatch.REMOVED
                    || !XlsSheetsMatcher.hasChanges(ourWorkbook,
                            ourWorkbook.getSheet(sheetName),
                            theirWorkbook,
                            theirWorkbook.getSheet(sheetName))) {
                return null;
            }
            return decideChangedInBoth(sheetName, ourMatch, theirToBase, mergePlans, conflicts);
        }
        var diffDecision = chooseChangedSide(ourMatch, theirMatch);
        if (diffDecision == DiffStatus.CONFLICT) {
            diffDecision = decideWhole(sheetName, SheetConflictReason.REMOVED_AND_CHANGED, conflicts);
        }
        return diffDecision;
    }

    /**
     * Decides how to merge a sheet changed in both revisions: as a resolution decided it for the whole sheet, cell by
     * cell or table by table when both revisions updated it, or as a conflict otherwise.
     */
    private DiffStatus decideChangedInBoth(String sheetName,
                                           XlsMatch match,
                                           Map<String, XlsMatch> theirToBase,
                                           Map<String, SheetMergePlan> mergePlans,
                                           Conflicts conflicts) {
        var decided = conflicts.decided(sheetName);
        if (decided != null) {
            return statusOf(decided);
        }
        if (match != XlsMatch.UPDATED) {
            return decideWhole(sheetName, SheetConflictReason.ADDED_IN_BOTH, conflicts);
        }
        var plan = plan(sheetName, theirToBase, conflicts);
        var tables = conflicts.tables();
        if (!tables.isEmpty()) {
            var refusal = conflicts.refusal();
            // Tables that touch may fit in another choice of versions, so only moved content is tried.
            if (refusal != SheetConflictReason.MOVED_CONTENT
                    || !refusedWhateverDecided(sheetName, theirToBase, tables)) {
                // A survey leaves it to the resolution to tell whether the tables fit together as they are resolved.
                return DiffStatus.CONFLICT;
            }
            // No version of the tables lifts the refusal, so the sheet is resolved only as a whole.
            conflicts.startSheet();
            return decideWhole(sheetName, refusal, conflicts);
        }
        if (plan.isEmpty()) {
            return decideWhole(sheetName, conflicts.refusal(), conflicts);
        }
        mergePlans.put(sheetName, plan.get());
        return DiffStatus.MERGED;
    }

    /**
     * Plans the merge of a sheet both revisions changed.
     *
     * @return what changes in OUR revision of the sheet, or nothing if the sheet stays a conflict
     */
    private Optional<SheetMergePlan> plan(String sheetName, Map<String, XlsMatch> theirToBase, Conflicts conflicts) {
        return XlsSheetMerger.plan(new Cursor(baseWorkbook, baseWorkbook.getSheet(sheetName)),
                new Cursor(ourWorkbook, ourWorkbook.getSheet(sheetName)),
                new Cursor(theirWorkbook, theirWorkbook.getSheet(sheetName)),
                name -> isChanged(theirToBase.get(name)),
                conflicts);
    }

    /**
     * Tells whether a sheet a survey refused stays refused whichever version its conflicting tables are taken from:
     * THEIR or BASE version of them all, OUR version being the one the survey went on with.
     */
    private boolean refusedWhateverDecided(String sheetName,
                                           Map<String, XlsMatch> theirToBase,
                                           List<TableConflict> tables) {
        return Stream.of(Resolution.THEIRS, Resolution.BASE).allMatch(side -> {
            var decisions = tables.stream()
                    .collect(Collectors.toMap(TableConflict::id, table -> side, (first, second) -> first));
            return plan(sheetName, theirToBase, Conflicts.resolution(decisions)).isEmpty();
        });
    }

    /**
     * Decides a sheet both revisions changed in ways that are resolved only for the sheet as a whole.
     *
     * @param reason why the sheet is resolved only as a whole; a strict merge may not tell it
     */
    private static DiffStatus decideWhole(String sheetName,
                                          @Nullable SheetConflictReason reason,
                                          Conflicts conflicts) {
        var decided = conflicts.decided(sheetName);
        if (decided != null) {
            return statusOf(decided);
        }
        if (reason != null) {
            conflicts.refuse(reason);
        }
        conflicts.unresolvedSheet(sheetName, reason);
        return DiffStatus.CONFLICT;
    }

    /**
     * The way the merge takes a sheet a resolution decided for as a whole.
     */
    private static DiffStatus statusOf(Resolution resolution) {
        return switch (resolution) {
            case OURS -> DiffStatus.OUR;
            case THEIRS -> DiffStatus.THEIR;
            case BASE -> DiffStatus.BASE;
        };
    }

    /**
     * A sheet as a survey lists it: resolved table by table when its tables conflict, and as a whole otherwise.
     */
    private SheetConflicts surveyed(String sheetName, DiffStatus status, Conflicts conflicts) {
        var conflicting = status == DiffStatus.CONFLICT;
        var tables = conflicting ? conflicts.tables() : List.<TableConflict>of();
        return new SheetConflicts(sheetName,
                status,
                conflicting && tables.isEmpty() ? conflicts.refusal() : null,
                ourWorkbook.getSheet(sheetName) != null,
                baseWorkbook.getSheet(sheetName) != null,
                theirWorkbook.getSheet(sheetName) != null,
                tables);
    }

    /**
     * The place a survey lists a sheet at: the place of the sheet in OUR revision, and after them the place of a
     * sheet OUR revision has not got in THEIR revision.
     */
    private int sheetOrder(String sheetName) {
        var ours = ourWorkbook.getSheetIndex(sheetName);
        return ours >= 0 ? ours : ourWorkbook.getNumberOfSheets() + theirWorkbook.getSheetIndex(sheetName);
    }

    /**
     * Tells whether a revision changed or created an item.
     */
    private static boolean isChanged(@Nullable XlsMatch match) {
        return match == XlsMatch.UPDATED || match == XlsMatch.CREATED;
    }

    private HSSFPaletteDiffResult calcPaletteDiff(Conflicts conflicts) {
        if (!hssf) {
            return new HSSFPaletteDiffResult(Map.of());
        }

        var diffResult = new EnumMap<DiffStatus, Set<Short>>(DiffStatus.class);

        var ourToBase = HSSFPaletteMatcher.matchPalette(toHSSFBook(baseWorkbook), toHSSFBook(ourWorkbook));
        var theirToBase = HSSFPaletteMatcher.matchPalette(toHSSFBook(baseWorkbook), toHSSFBook(theirWorkbook));
        var ourToTheir = HSSFPaletteMatcher.matchPalette(toHSSFBook(ourWorkbook), toHSSFBook(theirWorkbook));

        for (var entry : ourToBase.entrySet()) {
            var cIdx = entry.getKey();
            var ourMatchRes = entry.getValue();
            var theirMatchRes = theirToBase.get(cIdx);
            DiffStatus diffDecision;
            if (ourMatchRes == theirMatchRes) {
                // no changes for both workbooks, color was removed for both workbooks, or the same changes in both
                if (ourMatchRes == XlsMatch.EQUAL || ourMatchRes == XlsMatch.REMOVED
                        || ourToTheir.get(cIdx) == XlsMatch.EQUAL) {
                    continue;
                }
                diffDecision = DiffStatus.CONFLICT;
            } else {
                diffDecision = chooseChangedSide(ourMatchRes, theirMatchRes);
            }
            diffResult.computeIfAbsent(diffDecision, k -> new HashSet<>()).add(cIdx);
        }

        for (Short cIdx : theirToBase.keySet()) {
            if (!ourToBase.containsKey(cIdx)) {
                diffResult.computeIfAbsent(DiffStatus.THEIR, k -> new HashSet<>()).add(cIdx);
            }
        }

        decidePalette(diffResult, conflicts);
        return new HSSFPaletteDiffResult(diffResult);
    }

    /**
     * Resolves the colours of the palette both revisions changed differently as a resolution decided: the palette is
     * resolved as a whole, by OUR or THEIR revision.
     */
    private static void decidePalette(Map<DiffStatus, Set<Short>> diffResult, Conflicts conflicts) {
        var conflicting = diffResult.get(DiffStatus.CONFLICT);
        if (conflicting == null || conflicting.isEmpty()) {
            return;
        }
        var decided = conflicts.decided(WorkbookConflicts.PALETTE_ID);
        if (decided == Resolution.OURS || decided == Resolution.THEIRS) {
            var status = decided == Resolution.OURS ? DiffStatus.OUR : DiffStatus.THEIR;
            diffResult.computeIfAbsent(status, k -> new HashSet<>()).addAll(diffResult.remove(DiffStatus.CONFLICT));
        } else {
            conflicts.unresolved(WorkbookConflicts.PALETTE_ID);
        }
    }

    /**
     * Chooses the revision to take for an item that is not changed the same way in both revisions: their revision when
     * ours is unchanged, our revision when theirs is absent or unchanged, and a conflict otherwise.
     */
    private static DiffStatus chooseChangedSide(XlsMatch ourMatchRes, XlsMatch theirMatchRes) {
        if (ourMatchRes == XlsMatch.EQUAL) {
            return DiffStatus.THEIR;
        } else if (theirMatchRes == null || theirMatchRes == XlsMatch.EQUAL) {
            return DiffStatus.OUR;
        } else {
            return DiffStatus.CONFLICT;
        }
    }

    private static HSSFWorkbook toHSSFBook(StreamWorkbook workbook) {
        return (HSSFWorkbook) workbook.unwrap();
    }

    /**
     * Merge changes from {@code their} workbook and {@code our} workbook using {@link SheetDiffResult} to
     * {@code output} workbook
     *
     * @param our        our workbook
     * @param their      their workbook
     * @param diffResult difference result for these workbooks
     * @param output     output stream
     * @throws IOException if happen while merge
     */
    public static void merge(InputStream our,
                             InputStream their,
                             WorkbookDiffResult diffResult,
                             OutputStream output) throws IOException {
        merge(null, our, their, diffResult, output);
    }

    /**
     * Merges changes from {@code their} workbook and {@code our} workbook using the difference to {@code output}
     * workbook, reading {@code base} workbook when the difference takes a sheet or a table from BASE revision, as a
     * resolution may decide.
     *
     * @param base       base workbook, needed only when the difference takes something from it
     * @param our        our workbook
     * @param their      their workbook
     * @param diffResult difference result for these workbooks
     * @param output     output stream
     * @throws IOException if happen while merge
     */
    public static void merge(@Nullable InputStream base,
                             InputStream our,
                             InputStream their,
                             WorkbookDiffResult diffResult,
                             OutputStream output) throws IOException {
        if (diffResult.hasConflicts()) {
            throw new IllegalStateException("Can not merge because of conflicts.");
        }
        if (!diffResult.hasChangesToMerge()) {
            // Excel content identical so just take our revision
            IOUtils.copyAndClose(our, output);
            return;
        }
        var sheetDiffResult = diffResult.getSheetDiffResult();
        var paletteDifResult = diffResult.getPaletteDiffResult();
        var takesBase = sheetDiffResult.takesBase();
        if (takesBase && base == null) {
            throw new IllegalArgumentException("The base workbook is needed to take a sheet or a table from it.");
        }
        try (var ourBook = new StreamWorkbook(our, false);
             var theirBook = new StreamWorkbook(their, true);
             var baseBook = takesBase ? new StreamWorkbook(Objects.requireNonNull(base), true) : null) {
            if (sheetDiffResult.hasChangesToMerge()) {
                mergeSheets(ourBook, theirBook, baseBook, sheetDiffResult);
            }
            if (paletteDifResult.hasChangesToMerge()) {
                mergePalette(ourBook, theirBook, paletteDifResult);
            }

            ourBook.write(output);
        }
    }

    /**
     * Applies the sheets taken from THEIR or BASE workbook and the sheets merged from both revisions to OUR workbook,
     * and brings the values of its formulas up to date.
     *
     * @param baseBook BASE workbook, needed only when a sheet or a table is taken from it
     */
    private static void mergeSheets(StreamWorkbook ourBook,
                                    StreamWorkbook theirBook,
                                    @Nullable StreamWorkbook baseBook,
                                    SheetDiffResult sheetDiffResult) throws IOException {
        var formulas = new ArrayList<Cell>();
        for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.THEIR)) {
            copySheet(theirBook, ourBook, sheetName, formulas);
        }
        for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.BASE)) {
            copySheet(Objects.requireNonNull(baseBook), ourBook, sheetName, formulas);
        }
        var mergePlans = sheetDiffResult.getMergePlans();
        // a plan that moves cells reads them from OUR workbook as it was before the merge
        try (var ourSource = mergePlans.values().stream().anyMatch(SheetMergePlan::movesCells)
                ? ourBook.reopen()
                : null) {
            for (var entry : mergePlans.entrySet()) {
                var sheetName = entry.getKey();
                XlsSheetMerger.apply(new Cursor(theirBook, theirBook.getSheet(sheetName)),
                        new Cursor(ourBook, ourBook.getSheet(sheetName)),
                        ourSource == null ? null : new Cursor(ourSource, ourSource.getSheet(sheetName)),
                        baseBook == null ? null : new Cursor(baseBook, baseBook.getSheet(sheetName)),
                        entry.getValue());
            }
        }
        // evaluate all formula cells in the end
        if (mergePlans.isEmpty()) {
            var formulaEvaluator = ourBook.getCreationHelper().createFormulaEvaluator();
            formulas.forEach(formulaEvaluator::evaluateFormulaCell);
        } else {
            // a merged cell may be read by any formula of the workbook, the copied ones included
            XlsSheetMerger.recalculate(ourBook);
        }
        // optimize styles
        if (ourBook.unwrap() instanceof HSSFWorkbook ourHssfBook) {
            HSSFOptimiser.optimiseCellStyles(ourHssfBook);
        }
    }

    /**
     * Makes a sheet of the target workbook a copy of the sheet of the source workbook: copies the sheet into the one
     * of the target, which gets the sheet when it has none, or removes the sheet when the source has none.
     */
    private static void copySheet(StreamWorkbook source,
                                  StreamWorkbook target,
                                  String sheetName,
                                  Collection<Cell> formulas) throws IOException {
        var sourceSheet = source.getSheet(sheetName);
        var targetSheet = target.getSheet(sheetName);
        if (sourceSheet == null) {
            if (targetSheet != null) {
                target.removeSheetAt(target.getSheetIndex(targetSheet));
            }
        } else {
            XlsSheetCopier.copy(source,
                    sourceSheet,
                    target,
                    targetSheet == null ? target.createSheet(sheetName) : targetSheet,
                    formulas);
        }
    }

    /**
     * Copies the palette colors THEIR workbook changed, and the ones a resolution took from it, to our workbook.
     */
    private static void mergePalette(StreamWorkbook ourBook,
                                     StreamWorkbook theirBook,
                                     HSSFPaletteDiffResult paletteDifResult) {
        var ourHssfBook = (HSSFWorkbook) ourBook.unwrap();
        var ourPalette = ourHssfBook.getCustomPalette();
        var theirHssfBook = (HSSFWorkbook) theirBook.unwrap();
        var theirPalette = theirHssfBook.getCustomPalette();

        for (short i = FIRST_COLOR_INDEX; i < LAST_COLOR_INDEX; i++) {
            if (paletteDifResult.isTakenFromTheirs(i)) {
                var theirColor = theirPalette.getColor(i);
                // a colour THEIR palette has not got is left as OUR palette has it
                if (theirColor != null) {
                    var rgb = theirColor.getTriplet();
                    ourPalette.setColorAtIndex(i, (byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
                }
            }
        }
    }

    /**
     * Initialize merge analyzing
     *
     * @param base  base revision workbook
     * @param our   our revision workbook
     * @param their their revision
     * @return initialized class
     * @throws IOException if happen
     */
    public static XlsWorkbookMerger create(InputStream base, InputStream our, InputStream their) throws IOException {
        StreamWorkbook baseBook = null;
        StreamWorkbook ourBook = null;
        StreamWorkbook theirBook = null;
        try {
            baseBook = new StreamWorkbook(base, true);
            ourBook = new StreamWorkbook(our, true);
            theirBook = new StreamWorkbook(their, true);
        } catch (IOException | RuntimeException e) {
            // close all books in case of exception
            closeQuietly(baseBook);
            closeQuietly(ourBook);
            closeQuietly(theirBook);
            throw e;
        }
        return new XlsWorkbookMerger(baseBook, ourBook, theirBook);
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // safe to ignore: best-effort close
            }
        }
    }
}
