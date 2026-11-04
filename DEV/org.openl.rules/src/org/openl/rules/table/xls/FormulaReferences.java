package org.openl.rules.table.xls;

import java.util.ArrayList;

import lombok.RequiredArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFEvaluationWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.EvaluationWorkbook.ExternalSheetRange;
import org.apache.poi.ss.formula.ExternSheetReferenceToken;
import org.apache.poi.ss.formula.FormulaParser;
import org.apache.poi.ss.formula.FormulaParsingWorkbook;
import org.apache.poi.ss.formula.FormulaRenderer;
import org.apache.poi.ss.formula.FormulaRenderingWorkbook;
import org.apache.poi.ss.formula.FormulaType;
import org.apache.poi.ss.formula.ptg.Area3DPtg;
import org.apache.poi.ss.formula.ptg.AreaErrPtg;
import org.apache.poi.ss.formula.ptg.AreaPtgBase;
import org.apache.poi.ss.formula.ptg.Deleted3DPxg;
import org.apache.poi.ss.formula.ptg.DeletedArea3DPtg;
import org.apache.poi.ss.formula.ptg.DeletedRef3DPtg;
import org.apache.poi.ss.formula.ptg.NamePtg;
import org.apache.poi.ss.formula.ptg.NameXPxg;
import org.apache.poi.ss.formula.ptg.Ptg;
import org.apache.poi.ss.formula.ptg.Pxg;
import org.apache.poi.ss.formula.ptg.Pxg3D;
import org.apache.poi.ss.formula.ptg.Ref3DPtg;
import org.apache.poi.ss.formula.ptg.RefErrorPtg;
import org.apache.poi.ss.formula.ptg.RefPtgBase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFEvaluationWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.table.CellMove;
import org.openl.rules.table.GridRegion;

/**
 * Points the Excel formulas of a workbook at the cells of a sheet where they move, as Excel does when it shifts or
 * moves cells.
 *
 * <p>A formula is text, so it keeps its addresses when the cells at them move: without the change it would compute
 * from the cells that came there instead. A formula of any sheet of the workbook counts, wherever it stands. A
 * reference to a cell that stays where it is keeps its address; one to a cell taken away turns into {@code #REF!}.
 *
 * <p>The defined names of the workbook follow the cells as the formulas do. A formula that refers to the sheet, or to
 * a defined name, is computed again, so it keeps the value it computes from the cells as they are now.
 *
 * <p>Left as they are: a formula of an array, a formula Apache POI cannot read, a reference to a range of sheets or to
 * another workbook, and the conditional formatting and the data validation of a sheet. A formula that changes is
 * written as Apache POI writes it, so a structured reference to an Excel table in it becomes the address of the cells.
 */
@NullMarked
@RequiredArgsConstructor
final class FormulaReferences {

    private final FormulaParsingWorkbook parsing;
    private final String sheetName;
    private final CellMove move;
    /**
     * Whether a defined name of the workbook refers to the sheet: a formula naming no sheet may then refer to it
     * through the name.
     */
    private boolean namesReferToTheSheet;

    /**
     * Points the formulas of the workbook of a sheet at where the cells of the sheet move.
     *
     * @param sheet the sheet whose cells move
     * @param move  how they move
     */
    static void move(Sheet sheet, CellMove move) {
        var workbook = sheet.getWorkbook();
        var parsing = parsingOf(workbook);
        if (parsing == null) {
            return;
        }
        var references = new FormulaReferences(parsing, sheet.getSheetName(), move);
        for (var name : workbook.getAllNames()) {
            references.move(name);
        }
        var referring = new ArrayList<Cell>();
        for (var sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            for (var row : workbook.getSheetAt(sheetIndex)) {
                for (var cell : row) {
                    if (references.move(cell, sheetIndex)) {
                        referring.add(cell);
                    }
                }
            }
        }
        // The formulas are computed once all of them are rewritten, from the cells as they are now
        var evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        for (var cell : referring) {
            PoiExcelHelper.evaluateFormula(evaluator, cell);
        }
    }

    private static @Nullable FormulaParsingWorkbook parsingOf(Workbook workbook) {
        return switch (workbook) {
            case XSSFWorkbook xssf -> XSSFEvaluationWorkbook.create(xssf);
            case HSSFWorkbook hssf -> HSSFEvaluationWorkbook.create(hssf);
            default -> null;
        };
    }

    /**
     * Points a defined name at where the cells it names move. A name of a sheet names the cells of that sheet when it
     * names no sheet.
     */
    private void move(Name name) {
        var formula = name.getRefersToFormula();
        if (name.isFunctionName() || formula == null) {
            return;
        }
        var scope = name.getSheetIndex();
        Ptg[] tokens;
        try {
            tokens = FormulaParser.parse(formula, parsing, FormulaType.NAMEDRANGE, scope);
        } catch (RuntimeException e) {
            return;
        }
        var rewrite = rewrite(tokens, scope < 0 ? null : name.getSheetName());
        if (rewrite.changed()) {
            name.setRefersToFormula(FormulaRenderer.toFormulaString((FormulaRenderingWorkbook) parsing, tokens));
        }
        namesReferToTheSheet |= rewrite.refers();
    }

    /**
     * Points the formula of a cell at where the cells it refers to move.
     *
     * @return whether the formula refers to the sheet, so it is to be computed again
     */
    private boolean move(Cell cell, int sheetIndex) {
        if (cell.getCellType() != CellType.FORMULA || cell.isPartOfArrayFormulaGroup()) {
            return false;
        }
        var formulaSheet = cell.getSheet().getSheetName();
        Ptg[] tokens;
        try {
            var formula = cell.getCellFormula();
            // A formula of another sheet refers to the sheet by its name, or through a defined name
            if (!sheetName.equalsIgnoreCase(formulaSheet) && formula.indexOf('!') < 0 && !namesReferToTheSheet) {
                return false;
            }
            tokens = FormulaParser.parse(formula, parsing, FormulaType.CELL, sheetIndex, cell.getRowIndex());
        } catch (RuntimeException e) {
            return false;
        }
        var rewrite = rewrite(tokens, formulaSheet);
        if (rewrite.changed()) {
            cell.setCellFormula(FormulaRenderer.toFormulaString((FormulaRenderingWorkbook) parsing, tokens));
        }
        return rewrite.refers();
    }

    /**
     * What a rewrite found in a formula: whether it refers to the sheet, directly or through a defined name, and
     * whether one of its references moved.
     */
    private record Rewrite(boolean refers, boolean changed) {
    }

    /**
     * Points the references of a formula to the sheet at where their cells move, in place.
     *
     * @param formulaSheet the sheet a reference naming no sheet refers to, or {@code null} when there is none
     */
    private Rewrite rewrite(Ptg[] tokens, @Nullable String formulaSheet) {
        var refers = false;
        var changed = false;
        for (var i = 0; i < tokens.length; i++) {
            var token = tokens[i];
            var toTheSheet = refersToTheSheet(token, formulaSheet);
            if (toTheSheet) {
                tokens[i] = moved(token);
                changed |= tokens[i] != token;
            }
            refers |= toTheSheet || namesReferToTheSheet && (token instanceof NamePtg || token instanceof NameXPxg);
        }
        return new Rewrite(refers, changed);
    }

    /** Whether a token is a reference to cells of the sheet whose cells move. */
    private boolean refersToTheSheet(Ptg token, @Nullable String formulaSheet) {
        if (!(token instanceof RefPtgBase) && !(token instanceof AreaPtgBase)) {
            return false;
        }
        var sheet = switch (token) {
            case Pxg pxg -> sheetOf(pxg);
            case ExternSheetReferenceToken external -> sheetOf(external);
            default -> formulaSheet;
        };
        return sheetName.equalsIgnoreCase(sheet);
    }

    /** A reference to cells of the sheet after the move: the same token when none of its cells moves. */
    private Ptg moved(Ptg token) {
        return switch (token) {
            case RefPtgBase ref -> moved(ref);
            case AreaPtgBase area -> moved(area);
            default -> token;
        };
    }

    /**
     * The sheet a reference of an {@code .xlsx} formula names, or {@code null} for a sheet of another workbook or a
     * range of sheets.
     */
    private static @Nullable String sheetOf(Pxg pxg) {
        if (pxg.getExternalWorkbookNumber() > 0 || pxg instanceof Pxg3D range && range.getLastSheetName() != null) {
            return null;
        }
        return pxg.getSheetName();
    }

    /**
     * The sheet a reference of an {@code .xls} formula names, or {@code null} for a sheet of another workbook or a
     * range of sheets.
     */
    private @Nullable String sheetOf(ExternSheetReferenceToken external) {
        var sheet = ((HSSFEvaluationWorkbook) parsing).getExternalSheet(external.getExternSheetIndex());
        if (sheet == null || sheet.getWorkbookName() != null
                || sheet instanceof ExternalSheetRange range
                && !range.getFirstSheetName().equalsIgnoreCase(range.getLastSheetName())) {
            return null;
        }
        return sheet.getSheetName();
    }

    private Ptg moved(RefPtgBase ref) {
        var cell = new GridRegion(ref.getRow(), ref.getColumn(), ref.getRow(), ref.getColumn());
        var moved = move.moved(cell);
        if (moved == cell) {
            return ref;
        } else if (moved == null) {
            return deleted(ref);
        }
        var copy = (RefPtgBase) ref.copy();
        copy.setRow(moved.getTop());
        copy.setColumn(moved.getLeft());
        return copy;
    }

    private Ptg moved(AreaPtgBase area) {
        var region = new GridRegion(area.getFirstRow(),
                area.getFirstColumn(),
                area.getLastRow(),
                area.getLastColumn());
        var moved = move.moved(region);
        if (moved == region) {
            return area;
        } else if (moved == null) {
            return deleted(area);
        }
        var copy = (AreaPtgBase) area.copy();
        copy.setFirstRow(moved.getTop());
        copy.setFirstColumn(moved.getLeft());
        copy.setLastRow(moved.getBottom());
        copy.setLastColumn(moved.getRight());
        return copy;
    }

    /**
     * The {@code #REF!} a reference to cells taken away turns into, naming the sheet the reference named.
     */
    private static Ptg deleted(Ptg reference) {
        return switch (reference) {
            case Pxg pxg -> new Deleted3DPxg(pxg.getExternalWorkbookNumber(), pxg.getSheetName());
            case Ref3DPtg ptg -> new DeletedRef3DPtg(ptg.getExternSheetIndex());
            case Area3DPtg ptg -> new DeletedArea3DPtg(ptg.getExternSheetIndex());
            case AreaPtgBase ignored -> new AreaErrPtg();
            default -> new RefErrorPtg();
        };
    }
}
