package org.openl.rules.lang.xls.types.meta;

import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.NAMES;
import static org.openl.rules.cmatch.algorithm.MatchAlgorithmCompiler.VALUES;

import java.util.List;

import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.ColumnMatchBoundNode;
import org.openl.rules.cmatch.MatchNode;
import org.openl.rules.cmatch.SubValue;
import org.openl.rules.cmatch.TableRow;
import org.openl.rules.cmatch.algorithm.ArgumentsHelper;
import org.openl.rules.cmatch.algorithm.WeightAlgorithmCompiler;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.types.java.JavaOpenClass;

public class ColumnMatchMetaInfoReader extends AMethodMetaInfoReader<ColumnMatchBoundNode> {
    public ColumnMatchMetaInfoReader(ColumnMatchBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    protected CellMetaInfo getBodyMetaInfo(int row, int col) {
        if (isSpecialRow(row)) {
            return checkSpecialRowMetaInfo(row, col);
        } else {
            return checkValueMetaInfo(row, col);
        }
    }

    private boolean isSpecialRow(int rowNum) {
        var columnMatch = getBoundNode().getColumnMatch();
        var grid = columnMatch.getSyntaxNode().getTableBody().getSource().getGrid();

        var firstNameRowNum = getSpecialRowCount(columnMatch);
        var firstNameRegion = columnMatch.getRows().get(firstNameRowNum).get(NAMES)[0].getGridRegion();
        var cell = grid.getCell(firstNameRegion.getLeft(), firstNameRegion.getTop());

        return rowNum < cell.getAbsoluteRow();
    }

    private CellMetaInfo checkSpecialRowMetaInfo(int rowNum, int colNum) {
        var columnMatch = getBoundNode().getColumnMatch();

        var row0 = columnMatch.getRows().getFirst();

        var metaInfo = searchMetaInfo(columnMatch,
                rowNum,
                colNum,
                row0.get(VALUES),
                columnMatch.getReturnValues());
        if (metaInfo != NOT_FOUND) {
            return metaInfo;
        }

        if (getSpecialRowCount(columnMatch) > 1) {
            var totalScoreRow = columnMatch.getRows().get(WeightAlgorithmCompiler.ROW_TOTAL_SCORE_IDX);
            var totalScore = columnMatch.getTotalScore();
            metaInfo = searchMetaInfo(columnMatch,
                    rowNum,
                    colNum,
                    totalScoreRow.get(VALUES),
                    totalScore.getCheckValues());
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }

            var scoreRow = columnMatch.getRows().get(WeightAlgorithmCompiler.ROW_SCORE_IDX);
            metaInfo = searchMetaInfo(columnMatch,
                    rowNum,
                    colNum,
                    scoreRow.get(VALUES),
                    asObjects(columnMatch.getColumnScores()));
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        return null;
    }

    private static Object[] asObjects(int[] columnScores) {
        Object[] objects = new Object[columnScores.length];
        for (var i = 0; i < columnScores.length; i++) {
            objects[i] = columnScores[i];
        }
        return objects;
    }

    private CellMetaInfo searchMetaInfo(ColumnMatch columnMatch,
                                        int rowNum,
                                        int colNum,
                                        SubValue[] subValues,
                                        Object[] values) {
        var grid = columnMatch.getSyntaxNode().getTableBody().getSource().getGrid();
        for (var sv = 0; sv < subValues.length; sv++) {
            var subValue = subValues[sv];
            var region = subValue.getGridRegion();
            var cell = grid.getCell(region.getLeft(), region.getTop());

            if (isNeededCell(cell, rowNum, colNum)) {
                // "values" column
                // We must check actual value because we can find IntRange instead of Integer there.
                var value = values[sv];
                return value == null ? null : new CellMetaInfo(JavaOpenClass.getOpenClass(value.getClass()), false);
            }
        }

        return NOT_FOUND;
    }

    private CellMetaInfo checkValueMetaInfo(int rowNum, int colNum) {
        var columnMatch = getBoundNode().getColumnMatch();
        var grid = columnMatch.getSyntaxNode().getTableBody().getSource().getGrid();

        List<TableRow> rows = columnMatch.getRows();
        for (var i = getSpecialRowCount(columnMatch); i < rows.size(); i++) {
            var row = rows.get(i);

            var region = row.get(NAMES)[0].getGridRegion();
            var cell = grid.getCell(region.getLeft(), region.getTop());

            if (cell.getAbsoluteRow() != rowNum) {
                continue;
            }

            if (isNeededCell(cell, rowNum, colNum)) {
                // "names" column
                var argumentsHelper = new ArgumentsHelper(columnMatch.getHeader().getSignature());
                var domainOpenClass = argumentsHelper.generateDomainClassByArgNames();
                return new CellMetaInfo(domainOpenClass, false);
            }

            var subValues = row.get(VALUES);
            for (var sv = 0; sv < subValues.length; sv++) {
                var subValue = subValues[sv];
                region = subValue.getGridRegion();
                cell = grid.getCell(region.getLeft(), region.getTop());

                if (isNeededCell(cell, rowNum, colNum)) {
                    // "values" column
                    // We must check actual value because we can find IntRange instead of Integer there.
                    var checkValues = getCheckValues(columnMatch, i);
                    Object value = checkValues == null ? null : checkValues[sv];
                    return value == null ? null : new CellMetaInfo(JavaOpenClass.getOpenClass(value.getClass()), false);
                }
            }
        }

        return null;
    }

    private Object[] getCheckValues(ColumnMatch columnMatch, int rowIndex) {
        var checkTree = columnMatch.getCheckTree();
        if (checkTree == null) {
            return null;
        }
        List<MatchNode> children = checkTree.getChildren();
        return getObjects(children, rowIndex);
    }

    private static Object[] getObjects(List<MatchNode> children, int rowIndex) {
        for (MatchNode child : children) {
            if (child.getRowIndex() == rowIndex) {
                return child.getCheckValues();
            }
            Object[] objects = getObjects(child.getChildren(), rowIndex);
            if (objects != null) {
                return objects;
            }
        }
        return null;
    }

    private static int getSpecialRowCount(ColumnMatch columnMatch) {
        var alg = columnMatch.getAlgorithm();
        String nameOfAlgorithm = alg != null ? alg.getCode() : null;
        return "WEIGHTED".equals(nameOfAlgorithm) ? 3 : 1;
    }
}
