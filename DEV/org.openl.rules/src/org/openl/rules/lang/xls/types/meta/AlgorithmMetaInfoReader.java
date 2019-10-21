package org.openl.rules.lang.xls.types.meta;

import java.util.List;

import org.openl.binding.impl.NodeUsage;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.meta.StringValue;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.tbasic.*;
import org.openl.rules.tbasic.runtime.operations.OpenLEvaluationOperation;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgorithmMetaInfoReader extends AMethodMetaInfoReader<AlgorithmBoundNode> {
    private final Logger log = LoggerFactory.getLogger(AlgorithmMetaInfoReader.class);

    private int operationColumn = -1;

    public AlgorithmMetaInfoReader(AlgorithmBoundNode boundNode) {
        super(boundNode);
    }

    /**
     * Is invoked from binder
     */
    public void setOperationColumn(int operationColumn) {
        this.operationColumn = operationColumn;
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        ICell firstCell = getTableSyntaxNode().getTableBody().getCell(0, 2);
        if (operationColumn == -1) {
            log.error("Operation column is not initialized");
        } else {
            if (col == operationColumn) {
                return firstCell.getAbsoluteRow() <= row ? AlgorithmBuilder.CELL_META_INFO : null;
            }
        }

        Algorithm algorithm = getBoundNode().getAlgorithm();
        if (algorithm != null) {
            CellMetaInfo metaInfo = searchMetaInfo(row, col, algorithm.getAlgorithmSteps());
            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }

            for (AlgorithmSubroutineMethod method : algorithm.getSubroutines()) {
                metaInfo = searchMetaInfo(row, col, method.getAlgorithmSteps());
                if (metaInfo != NOT_FOUND) {
                    return metaInfo;
                }
            }
        }

        return null;
    }

    private CellMetaInfo searchMetaInfo(int row, int col, List<RuntimeOperation> operations) {
        if (operations == null) {
            return NOT_FOUND;
        }

        for (RuntimeOperation step : operations) {
            AlgorithmRow algorithmRow = step.getSourceCode().getAlgorithmRow();
            CellMetaInfo metaInfo = NOT_FOUND;
            if (AlgorithmBuilder.ACTION.equals(step.getSourceCode().getOperationFieldName())) {
                metaInfo = checkMetaInfo(row, col, step, algorithmRow.getAction());
            } else if (AlgorithmBuilder.CONDITION.equals(step.getSourceCode().getOperationFieldName())) {
                metaInfo = checkMetaInfo(row, col, step, algorithmRow.getCondition());
            }

            if (metaInfo != NOT_FOUND) {
                return metaInfo;
            }
        }

        return NOT_FOUND;
    }

    private CellMetaInfo checkMetaInfo(int row, int col, RuntimeOperation step, StringValue algorithmCell) {
        IOpenSourceCodeModule sourceModule = algorithmCell.getMetaInfo().getSource();
        if (sourceModule instanceof GridCellSourceCodeModule) {
            ICell cell = ((GridCellSourceCodeModule) sourceModule).getCell();
            if (isNeededCell(cell, row, col)) {
                // Found the cell. Return either meta info or null.
                if (step instanceof OpenLEvaluationOperation) {
                    IMethodCaller methodCaller = ((OpenLEvaluationOperation) step).getOpenLStatement();
                    if (methodCaller instanceof CompositeMethod) {
                        List<NodeUsage> nodeUsages = OpenLCellExpressionsCompiler
                            .getNodeUsages((CompositeMethod) methodCaller, sourceModule.getCode(), 0);

                        return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
                    }
                }

                return null;
            }
        }

        return NOT_FOUND;
    }
}
