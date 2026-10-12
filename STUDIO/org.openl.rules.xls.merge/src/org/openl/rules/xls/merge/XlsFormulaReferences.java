package org.openl.rules.xls.merge;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheetRange;
import org.apache.poi.ss.formula.ExternSheetReferenceToken;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.AreaPtgBase;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPtg;
import org.apache.poi.ss.formula.ptg.NameXPxg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Pxg;
import org.apache.poi.ss.formula.ptg.Pxg3D;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Tells whether the formulas and the defined names of a workbook refer to areas of a sheet.
 *
 * <p>A formula is text: it keeps referring to the same addresses when the content at them moves. A merge that moves
 * content therefore cannot keep a formula that refers to it, nor a formula that moves itself.
 *
 * <p>A formula that cannot be read is taken as referring to everything.
 */
@NullMarked
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class XlsFormulaReferences {

    private final Workbook workbook;
    private final FormulaParsingWorkbook parsing;
    private final String sheetName;
    private final Collection<CellRangeAddress> areas;
    /**
     * Whether a defined name refers to the areas, found on the first question.
     */
    private @Nullable Boolean namesRefer;

    /**
     * How far from where it is written a relative reference reaches: a condition of a data validation or of a
     * conditional formatting is written for the first cell of its first range and applies to every cell of all its
     * ranges, which may lie above, below or beside the first cell.
     */
    private record Spread(int top, int bottom, int left, int right) {
        static final Spread NONE = new Spread(0, 0, 0, 0);

        /**
         * The reach of a condition over its ranges, from the first cell of the first range.
         */
        static Spread of(CellRangeAddress[] ranges) {
            var first = ranges[0];
            var top = 0;
            var bottom = 0;
            var left = 0;
            var right = 0;
            for (CellRangeAddress range : ranges) {
                top = Math.min(top, range.getFirstRow() - first.getFirstRow());
                bottom = Math.max(bottom, range.getLastRow() - first.getFirstRow());
                left = Math.min(left, range.getFirstColumn() - first.getFirstColumn());
                right = Math.max(right, range.getLastColumn() - first.getFirstColumn());
            }
            return new Spread(top, bottom, left, right);
        }

        /**
         * The cells a reference reaches: a relative row or column moves with the cell the condition is checked for.
         */
        CellRangeAddress reach(int firstRow, boolean firstRowRelative, int lastRow, boolean lastRowRelative,
                               int firstColumn, boolean firstColumnRelative, int lastColumn,
                               boolean lastColumnRelative) {
            return new CellRangeAddress(Math.max(0, firstRow + (firstRowRelative ? top : 0)),
                    lastRow + (lastRowRelative ? bottom : 0),
                    Math.max(0, firstColumn + (firstColumnRelative ? left : 0)),
                    lastColumn + (lastColumnRelative ? right : 0));
        }
    }

    /**
     * Tells whether a formula of the workbook refers to an area of a sheet, or stands in such an area. Only the
     * formulas the merged workbook keeps count.
     *
     * @param workbook  the workbook to look through
     * @param sheetName the sheet the areas belong to
     * @param areas     the areas of the sheet
     * @param kept      tells whether the merged workbook keeps a formula cell
     */
    static boolean refersTo(Workbook workbook,
                            String sheetName,
                            Collection<CellRangeAddress> areas,
                            Predicate<Cell> kept) {
        return !areas.isEmpty() && references(workbook, sheetName, areas)
                .map(references -> references.formulasRefer(kept))
                .orElse(true);
    }

    /**
     * Tells whether anything of a workbook that keeps an address refers to areas of its sheet: a formula, a defined
     * name, and the data validations, the conditional formatting and the hyperlinks of the sheet, both where they
     * apply and in their conditions or the places they lead to.
     *
     * @param workbook the workbook to look through
     * @param sheet    the sheet the areas belong to
     * @param areas    the areas of the sheet
     */
    static boolean anythingRefersTo(Workbook workbook, Sheet sheet, Collection<CellRangeAddress> areas) {
        return !areas.isEmpty() && references(workbook, sheet.getSheetName(), areas)
                .map(references -> references.settingsRefer(sheet, workbook.getSheetIndex(sheet.getSheetName()))
                        || references.namesRefer()
                        || references.formulasRefer(cell -> true))
                .orElse(true);
    }

    /**
     * @return the references of a workbook, or nothing for a workbook whose formulas cannot be read
     */
    private static Optional<XlsFormulaReferences> references(Workbook workbook,
                                                             String sheetName,
                                                             Collection<CellRangeAddress> areas) {
        FormulaParsingWorkbook parsing = switch (workbook) {
            case XSSFWorkbook xssf -> XSSFEvaluationWorkbook.create(xssf);
            case HSSFWorkbook hssf -> HSSFEvaluationWorkbook.create(hssf);
            default -> null;
        };
        return Optional.ofNullable(parsing).map(p -> new XlsFormulaReferences(workbook, p, sheetName, areas));
    }

    private boolean settingsRefer(Sheet sheet, int sheetIndex) {
        for (DataValidation validation : sheet.getDataValidations()) {
            var constraint = validation.getValidationConstraint();
            if (conditionRefers(validation.getRegions().getCellRangeAddresses(),
                    FormulaType.CELL,
                    sheetIndex,
                    constraint.getFormula1(),
                    constraint.getFormula2())) {
                return true;
            }
        }
        var formatting = sheet.getSheetConditionalFormatting();
        for (var i = 0; i < formatting.getNumConditionalFormattings(); i++) {
            var conditional = formatting.getConditionalFormattingAt(i);
            for (var j = 0; j < conditional.getNumberOfRules(); j++) {
                var rule = conditional.getRule(j);
                if (conditionRefers(conditional.getFormattingRanges(),
                        FormulaType.CONDFORMAT,
                        sheetIndex,
                        rule.getFormula1(),
                        rule.getFormula2())) {
                    return true;
                }
            }
        }
        for (Hyperlink link : sheet.getHyperlinkList()) {
            if (inAreas(new CellRangeAddress(link.getFirstRow(),
                    link.getLastRow(),
                    link.getFirstColumn(),
                    link.getLastColumn())) || leadsToAreas(link, sheetIndex)) {
                return true;
            }
        }
        return false;
    }

    private boolean leadsToAreas(Hyperlink link, int sheetIndex) {
        var address = link.getAddress();
        return link.getType() == HyperlinkType.DOCUMENT && address != null
                && refers(address.startsWith("#") ? address.substring(1) : address,
                FormulaType.CELL,
                sheetIndex,
                0,
                Spread.NONE);
    }

    /**
     * Tells whether a condition lies over the areas, or refers to them from any cell of its ranges.
     */
    private boolean conditionRefers(CellRangeAddress[] ranges,
                                    FormulaType type,
                                    int sheetIndex,
                                    @Nullable String... formulas) {
        if (ranges.length == 0) {
            return false;
        }
        for (CellRangeAddress range : ranges) {
            if (inAreas(range)) {
                return true;
            }
        }
        var spread = Spread.of(ranges);
        for (String formula : formulas) {
            if (formula != null && !formula.isBlank() && refers(formula, type, sheetIndex, 0, spread)) {
                return true;
            }
        }
        return false;
    }

    private boolean namesRefer() {
        if (namesRefer == null) {
            // a name that refers to another name is checked when that name is reached
            namesRefer = Boolean.FALSE;
            namesRefer = anyNameRefers();
        }
        return namesRefer;
    }

    private boolean anyNameRefers() {
        for (Name name : workbook.getAllNames()) {
            var formula = name.getRefersToFormula();
            // a name whose area was deleted refers to nothing
            if (formula != null && !formula.contains("#REF!") && refers(formula,
                    FormulaType.NAMEDRANGE,
                    Math.max(name.getSheetIndex(), 0),
                    -1,
                    Spread.NONE)) {
                return true;
            }
        }
        return false;
    }

    private boolean formulasRefer(Predicate<Cell> kept) {
        for (var sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            if (formulasRefer(workbook.getSheetAt(sheetIndex), sheetIndex, kept)) {
                return true;
            }
        }
        return false;
    }

    private boolean formulasRefer(Sheet sheet, int sheetIndex, Predicate<Cell> kept) {
        var own = sheet.getSheetName().equalsIgnoreCase(sheetName);
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.FORMULA && kept.test(cell)
                        && formulaRefers(cell, own, sheetIndex)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tells whether a formula cell stands in the areas or refers to them. A formula of another sheet reaches the
     * sheet only by a reference that names a sheet, or by a defined name.
     */
    private boolean formulaRefers(Cell cell, boolean own, int sheetIndex) {
        if (own && areas.stream().anyMatch(area -> area.isInRange(cell))) {
            return true;
        }
        var formula = cell.getCellFormula();
        return (own || formula.indexOf('!') >= 0 || namesRefer())
                && refers(formula, FormulaType.CELL, sheetIndex, cell.getRowIndex(), Spread.NONE);
    }

    private boolean refers(String formula, FormulaType type, int sheetIndex, int rowIndex, Spread spread) {
        Ptg[] tokens;
        try {
            tokens = FormulaParser.parse(formula, parsing, type, sheetIndex, rowIndex);
        } catch (RuntimeException e) {
            return true;
        }
        var ownSheet = workbook.getSheetName(sheetIndex);
        for (Ptg token : tokens) {
            if (refers(token, ownSheet, spread)) {
                return true;
            }
        }
        return false;
    }

    private boolean refers(Ptg token, String ownSheet, Spread spread) {
        return switch (token) {
            // a formula refers to what its defined name refers to
            case NamePtg name -> namesRefer();
            case NameXPtg name -> namesRefer();
            case NameXPxg name -> namesRefer();
            case Pxg pxg -> refers(pxg, token, spread);
            case ExternSheetReferenceToken external -> refers(external, token, spread);
            default -> refersInSheet(token, ownSheet, spread);
        };
    }

    /**
     * Tells whether a reference of an .xlsx formula refers to the areas. The reference may name a sheet.
     */
    private boolean refers(Pxg pxg, Ptg token, Spread spread) {
        if (pxg.getExternalWorkbookNumber() > 0) {
            return false;
        } else if (pxg instanceof Pxg3D range && range.getLastSheetName() != null) {
            // a range of sheets may hold the sheet anywhere between its ends
            return true;
        }
        return refersInSheet(token, pxg.getSheetName(), spread);
    }

    /**
     * Tells whether a reference of an .xls formula that names a sheet refers to the areas.
     */
    private boolean refers(ExternSheetReferenceToken external, Ptg token, Spread spread) {
        var sheet = ((HSSFEvaluationWorkbook) parsing).getExternalSheet(external.getExternSheetIndex());
        if (sheet == null || sheet.getWorkbookName() != null) {
            // a sheet of another workbook
            return false;
        } else if (sheet instanceof ExternalSheetRange range
                && !range.getFirstSheetName().equalsIgnoreCase(range.getLastSheetName())) {
            // a range of sheets may hold the sheet anywhere between its ends
            return true;
        }
        return refersInSheet(token, sheet.getSheetName(), spread);
    }

    /**
     * Tells whether a reference to a sheet refers to the areas.
     */
    private boolean refersInSheet(Ptg token, String target, Spread spread) {
        if (!sheetName.equalsIgnoreCase(target)) {
            return false;
        }
        return switch (token) {
            case RefPtgBase ref -> inAreas(spread.reach(ref.getRow(), ref.isRowRelative(), ref.getRow(),
                    ref.isRowRelative(), ref.getColumn(), ref.isColRelative(), ref.getColumn(), ref.isColRelative()));
            case AreaPtgBase area -> inAreas(spread.reach(area.getFirstRow(), area.isFirstRowRelative(),
                    area.getLastRow(), area.isLastRowRelative(), area.getFirstColumn(), area.isFirstColRelative(),
                    area.getLastColumn(), area.isLastColRelative()));
            default -> false;
        };
    }

    private boolean inAreas(CellRangeAddress range) {
        return areas.stream().anyMatch(range::intersects);
    }
}
