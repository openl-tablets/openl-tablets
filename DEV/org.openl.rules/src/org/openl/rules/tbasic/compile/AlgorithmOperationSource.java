package org.openl.rules.tbasic.compile;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author User
 *
 */
public class AlgorithmOperationSource {
    private AlgorithmTreeNode sourceNode;
    private String operationFieldName;

    public AlgorithmOperationSource(AlgorithmTreeNode sourceNode, String operationFieldName) {
        this.sourceNode = sourceNode;
        this.operationFieldName = operationFieldName;
    }

    public IGridRegion getGridRegion() {
        IGridRegion sourceRegion = null;

        if (operationFieldName != null) {
            sourceRegion = sourceNode.getAlgorithmRow().getValueGridRegion(operationFieldName);
        }

        // if source is not value source or not found
        if (sourceRegion == null) {
            sourceRegion = sourceNode.getAlgorithmRow().getGridRegion();
        }

        return sourceRegion;
    }

    public String getOperationName() {
        return sourceNode.getAlgorithmRow().getOperation().getValue();
    }

    public int getRowNumber() {
        return sourceNode.getAlgorithmRow().getRowNumber();
    }

    public IOpenSourceCodeModule getSourceModule() {
        return sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule();
    }

    public String getSourceUri() {
        return sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule().getUri();
    }

    public AlgorithmRow getAlgorithmRow() {
        return sourceNode.getAlgorithmRow();
    }

    public String getOperationFieldName() {
        return operationFieldName;
    }
}
