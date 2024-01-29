package org.openl.rules.xls.merge;

import static org.openl.rules.xls.merge.HSSFPaletteMatcher.FIRST_COLOR_INDEX;
import static org.openl.rules.xls.merge.HSSFPaletteMatcher.LAST_COLOR_INDEX;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFOptimiser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.xls.merge.diff.DiffStatus;
import org.openl.rules.xls.merge.diff.HSSFPaletteDiffResult;
import org.openl.rules.xls.merge.diff.SheetDiffResult;
import org.openl.rules.xls.merge.diff.WorkbookDiffResult;
import org.openl.rules.xls.merge.diff.XlsMatch;
import org.openl.util.IOUtils;

/**
 * This services helps to merge two conflicted revisions based on base revision
 * 
 * @author Vladyslav Pikus
 */
public class XlsWorkbookMerger implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(XlsWorkbookMerger.class);

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
        Map<DiffStatus, Set<String>> diffResult = new HashMap<>();
        Map<String, XlsMatch> ourToBase = XlsWorkbooksMatcher.match(baseWorkbook, ourWorkbook);
        Map<String, XlsMatch> theirToBase = XlsWorkbooksMatcher.match(baseWorkbook, theirWorkbook);

        final Function<DiffStatus, Set<String>> initGroupValue = key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (String sheetName : ourToBase.keySet()) {
            XlsMatch ourMatchRes = ourToBase.get(sheetName);
            XlsMatch theirMatchRes = theirToBase.get(sheetName);
            DiffStatus diffDecision;
            if (ourMatchRes != null && ourMatchRes == theirMatchRes) {
                if (ourMatchRes == XlsMatch.EQUAL) {
                    // no changes for both workbooks
                    continue;
                } else if (ourMatchRes == XlsMatch.REMOVED) {
                    // sheet was removed for both workbooks
                    continue;
                } else {
                    boolean hasChanges = XlsSheetsMatcher.hasChanges(ourWorkbook,
                        ourWorkbook.getSheet(sheetName),
                        theirWorkbook,
                        theirWorkbook.getSheet(sheetName));
                    if (!hasChanges) {
                        continue;
                    }
                    diffDecision = DiffStatus.CONFLICT;
                }
            } else {
                if (ourMatchRes == XlsMatch.EQUAL) {
                    diffDecision = DiffStatus.THEIR;
                } else if (theirMatchRes == null || theirMatchRes == XlsMatch.EQUAL) {
                    diffDecision = DiffStatus.OUR;
                } else {
                    diffDecision = DiffStatus.CONFLICT;
                }
            }
            LOG.debug("{} resolution is chosen for '{}' sheet", diffDecision.name(), sheetName);
            diffResult.computeIfAbsent(diffDecision, initGroupValue).add(sheetName);
        }
        for (String sheetName : theirToBase.keySet()) {
            if (!ourToBase.containsKey(sheetName)) {
                diffResult.computeIfAbsent(DiffStatus.THEIR, initGroupValue).add(sheetName);
            }
        }

        var paletteDiff = calcPaletteDiff();
        return new WorkbookDiffResult(new SheetDiffResult(diffResult, theirToBase), paletteDiff);
    }

    private HSSFPaletteDiffResult calcPaletteDiff() {
        if (!hssf) {
            return new HSSFPaletteDiffResult(Collections.emptyMap(), Collections.emptyMap());
        }

        var diffResult = new HashMap<DiffStatus, Set<Short>>();

        var ourToBase = HSSFPaletteMatcher.matchPalette(toHSSFBook(baseWorkbook), toHSSFBook(ourWorkbook));
        var theirToBase = HSSFPaletteMatcher.matchPalette(toHSSFBook(baseWorkbook), toHSSFBook(theirWorkbook));
        var ourToTheir = HSSFPaletteMatcher.matchPalette(toHSSFBook(ourWorkbook), toHSSFBook(theirWorkbook));

        for (Short cIdx : ourToBase.keySet()) {
            XlsMatch ourMatchRes = ourToBase.get(cIdx);
            XlsMatch theirMatchRes = theirToBase.get(cIdx);
            DiffStatus diffDecision;
            if (ourMatchRes == theirMatchRes) {
                if (ourMatchRes == XlsMatch.EQUAL) {
                    // no changes for both workbooks
                    continue;
                } else if (ourMatchRes == XlsMatch.REMOVED) {
                    // sheet was removed for both workbooks
                    continue;
                } else {
                    XlsMatch match = ourToTheir.get(cIdx);
                    if (match == XlsMatch.EQUAL) {
                        continue;
                    }
                    diffDecision = DiffStatus.CONFLICT;
                }
            } else {
                if (ourMatchRes == XlsMatch.EQUAL) {
                    diffDecision = DiffStatus.THEIR;
                } else if (theirMatchRes == null || theirMatchRes == XlsMatch.EQUAL) {
                    diffDecision = DiffStatus.OUR;
                } else {
                    diffDecision = DiffStatus.CONFLICT;
                }
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

    private static HSSFWorkbook toHSSFBook(StreamWorkbook workbook) {
        return (HSSFWorkbook) workbook.unwrap();
    }

    /**
     * Merge changes from {@code their} workbook and {@code our} workbook using {@link SheetDiffResult} to
     * {@code output} workbook
     *
     * @param our our workbook
     * @param their their workbook
     * @param diffResult difference result for these workbooks
     * @param output output stream
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
        try (StreamWorkbook ourBook = new StreamWorkbook(our, false);
                StreamWorkbook theirBook = new StreamWorkbook(their, true)) {
            if (sheetDiffResult.hasChangesToMerge()) {
                List<Cell> formulas = new ArrayList<>();
                for (String sheetName : sheetDiffResult.getDiffSheets(DiffStatus.THEIR)) {
                    switch (sheetDiffResult.getTheirMatchResult(sheetName)) {
                        case UPDATED:
                            XlsSheetCopier.copy(theirBook,
                                theirBook.getSheet(sheetName),
                                ourBook,
                                ourBook.getSheet(sheetName),
                                formulas);
                            break;
                        case CREATED:
                            XlsSheetCopier.copy(theirBook,
                                theirBook.getSheet(sheetName),
                                ourBook,
                                ourBook.createSheet(sheetName),
                                formulas);
                            break;
                        case REMOVED:
                            int sheetIdx = ourBook.getSheetIndex(sheetName);
                            ourBook.removeSheetAt(sheetIdx);
                            break;
                        default:
                            throw new IllegalStateException("Failed to merge.");
                    }
                }
                // evaluate all formula cells in the end
                FormulaEvaluator formulaEvaluator = ourBook.getCreationHelper().createFormulaEvaluator();
                formulas.forEach(formulaEvaluator::evaluateFormulaCell);
                // optimize styles
                if (ourBook.unwrap() instanceof HSSFWorkbook) {
                    HSSFOptimiser.optimiseCellStyles((HSSFWorkbook) ourBook.unwrap());
                }
            }
            if (paletteDifResult.hasChangesToMerge()) {
                var ourHssfBook = (HSSFWorkbook) ourBook.unwrap();
                var ourPalette = ourHssfBook.getCustomPalette();
                var theirHssfBook = (HSSFWorkbook) theirBook.unwrap();
                var theirPalette = theirHssfBook.getCustomPalette();

                for (short i = FIRST_COLOR_INDEX; i < LAST_COLOR_INDEX; i++) {
                    XlsMatch theirMatchResult = paletteDifResult.getTheirMatchResult(i);
                    if (theirMatchResult == XlsMatch.UPDATED || theirMatchResult == XlsMatch.CREATED) {
                        var theirColor = theirPalette.getColor(i);
                        var rgb = theirColor.getTriplet();
                        ourPalette.setColorAtIndex(i, (byte) rgb[0], (byte) rgb[1], (byte) rgb[2]);
                    }
                }
            }

            ourBook.write(output);
        }
    }

    /**
     * Initialize merge analyzing
     *
     * @param base base revision workbook
     * @param our our revision workbook
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
            }
        }
    }
}
