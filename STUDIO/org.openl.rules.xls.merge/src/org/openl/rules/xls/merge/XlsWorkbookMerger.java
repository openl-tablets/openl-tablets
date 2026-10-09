package org.openl.rules.xls.merge;

import static org.openl.rules.xls.merge.HSSFPaletteMatcher.FIRST_COLOR_INDEX;
import static org.openl.rules.xls.merge.HSSFPaletteMatcher.LAST_COLOR_INDEX;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFOptimiser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.HSSFPaletteDiffResult;
import org.openl.rules.xls.merge.diff.SheetDiffResult;
import org.openl.rules.xls.merge.diff.SheetMergePlan;
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
        var diffResult = new EnumMap<DiffStatus, Set<String>>(DiffStatus.class);
        Map<String, XlsMatch> ourToBase = XlsWorkbooksMatcher.match(baseWorkbook, ourWorkbook);
        Map<String, XlsMatch> theirToBase = XlsWorkbooksMatcher.match(baseWorkbook, theirWorkbook);

        var mergePlans = new TreeMap<String, SheetMergePlan>(String.CASE_INSENSITIVE_ORDER);

        final Function<DiffStatus, Set<String>> initGroupValue = key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (var entry : ourToBase.entrySet()) {
            var sheetName = entry.getKey();
            var ourMatchRes = entry.getValue();
            var theirMatchRes = theirToBase.get(sheetName);
            DiffStatus diffDecision;
            if (ourMatchRes != null && ourMatchRes == theirMatchRes) {
                // no changes for both workbooks, sheet was removed for both workbooks, or the same changes in both
                if (ourMatchRes == XlsMatch.EQUAL || ourMatchRes == XlsMatch.REMOVED
                        || !XlsSheetsMatcher.hasChanges(ourWorkbook,
                                ourWorkbook.getSheet(sheetName),
                                theirWorkbook,
                                theirWorkbook.getSheet(sheetName))) {
                    continue;
                }
                diffDecision = decideChangedInBoth(sheetName, ourMatchRes, mergePlans);
            } else {
                diffDecision = chooseChangedSide(ourMatchRes, theirMatchRes);
            }
            log.debug("{} resolution is chosen for '{}' sheet", diffDecision.name(), sheetName);
            diffResult.computeIfAbsent(diffDecision, initGroupValue).add(sheetName);
        }
        for (String sheetName : theirToBase.keySet()) {
            if (!ourToBase.containsKey(sheetName)) {
                diffResult.computeIfAbsent(DiffStatus.THEIR, initGroupValue).add(sheetName);
            }
        }

        var paletteDiff = calcPaletteDiff();
        return new WorkbookDiffResult(new SheetDiffResult(diffResult, theirToBase, mergePlans), paletteDiff);
    }

    /**
     * Decides how to merge a sheet changed in both revisions: cell by cell, when both revisions updated it and the
     * changes do not conflict, or as a conflict otherwise.
     */
    private DiffStatus decideChangedInBoth(String sheetName, XlsMatch match, Map<String, SheetMergePlan> mergePlans) {
        if (match != XlsMatch.UPDATED) {
            return DiffStatus.CONFLICT;
        }
        var plan = XlsSheetMerger.plan(new Cursor(baseWorkbook, baseWorkbook.getSheet(sheetName)),
                new Cursor(ourWorkbook, ourWorkbook.getSheet(sheetName)),
                new Cursor(theirWorkbook, theirWorkbook.getSheet(sheetName)));
        if (plan.isEmpty()) {
            return DiffStatus.CONFLICT;
        }
        mergePlans.put(sheetName, plan.get());
        return DiffStatus.MERGED;
    }

    private HSSFPaletteDiffResult calcPaletteDiff() {
        if (!hssf) {
            return new HSSFPaletteDiffResult(Map.of(), Map.of());
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

        return new HSSFPaletteDiffResult(diffResult, theirToBase);
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
        try (var ourBook = new StreamWorkbook(our, false);
             var theirBook = new StreamWorkbook(their, true)) {
            if (sheetDiffResult.hasChangesToMerge()) {
                mergeSheets(ourBook, theirBook, sheetDiffResult);
            }
            if (paletteDifResult.hasChangesToMerge()) {
                mergePalette(ourBook, theirBook, paletteDifResult);
            }

            ourBook.write(output);
        }
    }

    /**
     * Applies the sheets taken from THEIR workbook and the sheets merged cell by cell to OUR workbook, and brings the
     * values of its formulas up to date.
     */
    private static void mergeSheets(StreamWorkbook ourBook,
                                    StreamWorkbook theirBook,
                                    SheetDiffResult sheetDiffResult) throws IOException {
        var formulas = new ArrayList<Cell>();
        for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.THEIR)) {
            switch (sheetDiffResult.getTheirMatchResult(sheetName)) {
                case UPDATED -> XlsSheetCopier.copy(theirBook,
                        theirBook.getSheet(sheetName),
                        ourBook,
                        ourBook.getSheet(sheetName),
                        formulas);
                case CREATED -> XlsSheetCopier.copy(theirBook,
                        theirBook.getSheet(sheetName),
                        ourBook,
                        ourBook.createSheet(sheetName),
                        formulas);
                case REMOVED -> {
                    var sheetIdx = ourBook.getSheetIndex(sheetName);
                    ourBook.removeSheetAt(sheetIdx);
                }
                default -> throw new IllegalStateException("Failed to merge.");
            }
        }
        var mergePlans = sheetDiffResult.getMergePlans();
        for (var entry : mergePlans.entrySet()) {
            var sheetName = entry.getKey();
            XlsSheetMerger.apply(new Cursor(theirBook, theirBook.getSheet(sheetName)),
                    new Cursor(ourBook, ourBook.getSheet(sheetName)),
                    entry.getValue());
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
     * Copies the palette colors updated or created in their workbook to our workbook.
     */
    private static void mergePalette(StreamWorkbook ourBook,
                                     StreamWorkbook theirBook,
                                     HSSFPaletteDiffResult paletteDifResult) {
        var ourHssfBook = (HSSFWorkbook) ourBook.unwrap();
        var ourPalette = ourHssfBook.getCustomPalette();
        var theirHssfBook = (HSSFWorkbook) theirBook.unwrap();
        var theirPalette = theirHssfBook.getCustomPalette();

        for (short i = FIRST_COLOR_INDEX; i < LAST_COLOR_INDEX; i++) {
            var theirMatchResult = paletteDifResult.getTheirMatchResult(i);
            if (theirMatchResult == XlsMatch.UPDATED || theirMatchResult == XlsMatch.CREATED) {
                var theirColor = theirPalette.getColor(i);
                var rgb = theirColor.getTriplet();
                ourPalette.setColorAtIndex(i, (byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
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
