package org.openl.rules.tbasic.compile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.rules.tbasic.AlgorithmRow;
import org.openl.rules.tbasic.AlgorithmTreeNode;
import org.openl.source.IOpenSourceCodeModule;

/**
 * @author User
 */
@RequiredArgsConstructor
public class AlgorithmOperationSource {
    private final AlgorithmTreeNode sourceNode;
    @Getter
    private final String operationFieldName;

    public String getOperationName() {
        return sourceNode.getAlgorithmRow().getOperation().getValue();
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
}
