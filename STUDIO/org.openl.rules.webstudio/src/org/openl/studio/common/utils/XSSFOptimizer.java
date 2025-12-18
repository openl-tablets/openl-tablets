package org.openl.studio.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyle;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyles;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XSSFOptimizer {
    private static final Logger LOG = LoggerFactory.getLogger(XSSFOptimizer.class);

    /**
     * Keeps original behavior: only compacts styleXfs (cellStyleXfs) and related named styles (cellStyles).
     * Does NOT remove unused cellXfs (cellXfs / cell formats referenced by sheet cells).
     */
    public static void removeUnusedStyles(XSSFWorkbook workbook) {
        removeUnusedStyles(workbook, false);
    }

    /**
     * @param removeUnusedCellXfs if true, removes unused <xf> entries from <cellXfs> and rewrites all references:
     *                            CTCell.s, CTRow.s, CTCol.style.
     */
    public static void removeUnusedStyles(XSSFWorkbook workbook, boolean removeUnusedCellXfs) {
        if (workbook == null) {
            return;
        }

        long startedAtNs = System.nanoTime();
        int sheets = workbook.getNumberOfSheets();

        LOG.info("Starting style optimization... (removeUnusedCellXfs={}, sheets={})", removeUnusedCellXfs, sheets);

        try {
            StylesTable stylesSource = workbook.getStylesSource();
            if (stylesSource == null || stylesSource.getCTStylesheet() == null) {
                LOG.info("No styles table found. Skipping optimization.");
                return;
            }

            // NOTE: Using reflection is fragile across POI versions. We keep it but fail safely.
            @SuppressWarnings("unchecked")
            List<CTXf> cellXfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "xfs", true);        // <cellXfs>
            @SuppressWarnings("unchecked")
            List<CTXf> styleXfs = (List<CTXf>) FieldUtils.readDeclaredField(stylesSource, "styleXfs", true);  // <cellStyleXfs>

            if (cellXfs == null || styleXfs == null) {
                LOG.info("Styles lists are not accessible. Skipping optimization.");
                return;
            }

            // Ensure <cellStyles> exists and is attached to the stylesheet (important when it was null).
            var ct = stylesSource.getCTStylesheet();
            CTCellStyles cellStyles = ct.getCellStyles();
            if (cellStyles == null) {
                cellStyles = CTCellStyles.Factory.newInstance();
                ct.setCellStyles(cellStyles);
            }

            List<CTCellStyle> existingNamedStyles = new ArrayList<>(cellStyles.getCellStyleList());

            LOG.info("Exists: cellXfs={}, styleXfs={}, namedStyles={}",
                    cellXfs.size(), styleXfs.size(), existingNamedStyles.size());

            // 1) Optionally compact <cellXfs> (cell formats) by usage from sheets and rewrite all references.
            if (removeUnusedCellXfs && !cellXfs.isEmpty()) {
                compactCellXfsAndRewriteReferences(workbook, cellXfs);
                LOG.info("After cellXfs compaction: cellXfs={}", cellXfs.size());
            }

            // 2) Compact <cellStyleXfs> (styleXfs) based on xfId references from remaining <cellXfs>.
            compactStyleXfsAndNamedStyles(cellXfs, styleXfs, cellStyles, existingNamedStyles);

            LOG.info("Finished style optimization. Result: cellXfs={}, styleXfs={}, namedStyles={}",
                    cellXfs.size(), styleXfs.size(), cellStyles.sizeOfCellStyleArray());

        } catch (ReflectiveOperationException | RuntimeException e) {
            // POI internals may change. In that case: do not partially modify the workbook further.
            LOG.error("Style optimization failed: {}", e.getMessage(), e);
        } finally {
            long elapsedNs = System.nanoTime() - startedAtNs;
            LOG.info("Style optimization time: {} ms", TimeUnit.NANOSECONDS.toMillis(elapsedNs));
        }
    }

    /**
     * Removes unused <xf> entries from <cellXfs> and rewrites references:
     * - CTCell.s
     * - CTRow.s
     * - CTCol.style
     */
    private static void compactCellXfsAndRewriteReferences(XSSFWorkbook workbook, List<CTXf> cellXfs) {
        int size = cellXfs.size();
        boolean[] used = new boolean[size];

        // Excel default style index 0 is often implicitly used even if no explicit "s" attributes exist.
        used[0] = true;

        // Detect usage from sheets (cells, rows, columns).
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            if (sheet == null || sheet.getCTWorksheet() == null) {
                continue;
            }

            // Columns: <cols><col style="..."/></cols>
            // NOTE: CTWorksheet has getColsList() (may be empty), no isSetCols().
            for (CTCols cols : sheet.getCTWorksheet().getColsList()) {
                for (CTCol col : cols.getColList()) {
                    if (col.isSetStyle()) {
                        markUsedIndex(used, (int) col.getStyle());
                    }
                }
            }

            // Rows + cells: <sheetData><row s="..."><c s="..."/></row></sheetData>
            CTSheetData sheetData = sheet.getCTWorksheet().getSheetData();
            if (sheetData == null) {
                continue;
            }

            for (CTRow row : sheetData.getRowList()) {
                if (row.isSetS()) {
                    markUsedIndex(used, (int) row.getS());
                }
                for (CTCell cell : row.getCList()) {
                    if (cell.isSetS()) {
                        markUsedIndex(used, (int) cell.getS());
                    }
                }
            }
        }

        // Build mapping oldIndex -> newIndex and the new compacted list, preserving order.
        int[] map = new int[size];
        int newIndex = 0;
        List<CTXf> newCellXfs = new ArrayList<>();

        for (int oldIndex = 0; oldIndex < size; oldIndex++) {
            if (!used[oldIndex]) {
                map[oldIndex] = -1;
                continue;
            }
            map[oldIndex] = newIndex++;
            newCellXfs.add(cellXfs.get(oldIndex));
        }

        // Rewrite references across sheets (cells/rows/cols).
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            XSSFSheet sheet = workbook.getSheetAt(i);
            if (sheet == null || sheet.getCTWorksheet() == null) {
                continue;
            }

            // Columns
            for (CTCols cols : sheet.getCTWorksheet().getColsList()) {
                for (CTCol col : cols.getColList()) {
                    if (col.isSetStyle()) {
                        col.setStyle(remapIndexOrDefault(map, (int) col.getStyle()));
                    }
                }
            }

            // Rows + cells
            CTSheetData sheetData = sheet.getCTWorksheet().getSheetData();
            if (sheetData == null) {
                continue;
            }

            for (CTRow row : sheetData.getRowList()) {
                if (row.isSetS()) {
                    row.setS(remapIndexOrDefault(map, (int) row.getS()));
                }
                for (CTCell cell : row.getCList()) {
                    if (cell.isSetS()) {
                        cell.setS(remapIndexOrDefault(map, (int) cell.getS()));
                    }
                }
            }
        }

        // Replace list content in-place.
        cellXfs.clear();
        cellXfs.addAll(newCellXfs);
    }

    /**
     * Compacts <cellStyleXfs> based on xfId references from <cellXfs>, and compacts named styles (<cellStyles>).
     * Also rewrites CTXf.xfId in cellXfs to new styleXfs indices.
     */
    private static void compactStyleXfsAndNamedStyles(List<CTXf> cellXfs,
                                                      List<CTXf> styleXfs,
                                                      CTCellStyles cellStyles,
                                                      List<CTCellStyle> existingNamedStyles) {
        int styleSize = styleXfs.size();
        if (styleSize == 0) {
            // Nothing to compact; still normalize named styles to empty.
            cellStyles.setCount(0);
            cellStyles.setCellStyleArray(new CTCellStyle[0]);
            return;
        }

        boolean[] usedStyle = new boolean[styleSize];

        // Mark used styleXfs by xfId referenced from each cellXf.
        for (CTXf cellXf : cellXfs) {
            int oldStyleId = (int) cellXf.getXfId();
            if (oldStyleId >= 0 && oldStyleId < styleSize) {
                usedStyle[oldStyleId] = true;
            }
        }

        // Build mapping oldStyleId -> newStyleId and create compacted styleXfs list.
        int[] styleMap = new int[styleSize];
        int newId = 0;
        List<CTXf> newStyleXfs = new ArrayList<>();

        for (int oldId = 0; oldId < styleSize; oldId++) {
            if (!usedStyle[oldId]) {
                styleMap[oldId] = -1;
                continue;
            }
            styleMap[oldId] = newId;
            CTXf styleXf = styleXfs.get(oldId);
            // Keep xfId coherent with new index. (Some consumers assume this matches its position.)
            styleXf.setXfId(newId);
            newStyleXfs.add(styleXf);
            newId++;
        }

        // Rewrite xfId in all cellXfs.
        for (CTXf cellXf : cellXfs) {
            int oldStyleId = (int) cellXf.getXfId();
            if (oldStyleId >= 0 && oldStyleId < styleMap.length && styleMap[oldStyleId] >= 0) {
                cellXf.setXfId(styleMap[oldStyleId]);
            } else {
                // Fallback to 0 (default) to avoid broken references.
                cellXf.setXfId(0);
            }
        }

        // Compact named styles: keep only those that reference still-used styleXf IDs and remap them.
        List<CTCellStyle> newNamedStyles = new ArrayList<>();
        for (CTCellStyle named : existingNamedStyles) {
            int oldStyleId = (int) named.getXfId();
            if (oldStyleId >= 0 && oldStyleId < styleMap.length && styleMap[oldStyleId] >= 0) {
                named.setXfId(styleMap[oldStyleId]);
                newNamedStyles.add(named);
            }
        }

        // Replace styleXfs in-place.
        styleXfs.clear();
        styleXfs.addAll(newStyleXfs);

        // Rewrite <cellStyles>.
        cellStyles.setCount(newNamedStyles.size());
        cellStyles.setCellStyleArray(newNamedStyles.toArray(new CTCellStyle[0]));

        LOG.info("Used: styleXfs={}, namedStyles={}", newStyleXfs.size(), newNamedStyles.size());
    }

    private static void markUsedIndex(boolean[] used, int index) {
        if (index >= 0 && index < used.length) {
            used[index] = true;
        }
    }

    private static long remapIndexOrDefault(int[] map, int oldIndex) {
        if (oldIndex >= 0 && oldIndex < map.length) {
            int newIndex = map[oldIndex];
            if (newIndex >= 0) {
                return newIndex;
            }
        }
        // Default to 0 to keep workbook consistent even if we see an unexpected index.
        return 0;
    }

    private XSSFOptimizer() {
    }
}
