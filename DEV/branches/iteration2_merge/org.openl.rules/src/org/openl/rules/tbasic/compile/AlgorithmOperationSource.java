/**
 * 
 */
package org.openl.rules.tbasic.compile;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.AlgorithmTreeNode;

/**
 * @author User
 *
 */
public class AlgorithmOperationSource {
    private AlgorithmTreeNode sourceNode;
    private String valueName;

    public AlgorithmOperationSource(AlgorithmTreeNode sourceNode, String valueName) {
        this.sourceNode = sourceNode;
        this.valueName = valueName;
    }

    public String getSourceUri() {
        return sourceNode.getAlgorithmRow().getOperation().asSourceCodeModule().getUri(0);
    }
    
    public IGridRegion getGridRegion() {
        IGridRegion sourceRegion = null;
        
        if (valueName != null) {
            sourceRegion = sourceNode.getAlgorithmRow().getValueGridRegion(valueName);
        } 
        
        // if source is not value source or not found
        if (sourceRegion == null) {
            sourceRegion =  sourceNode.getAlgorithmRow().getGridRegion();
        }
        
        return sourceRegion;
    }
    
    public String getOperationName() {
        String operationName = sourceNode.getAlgorithmRow().getOperation().getValue();
        if (valueName != null) {
            operationName += " " + valueName;
        } 
        return operationName;
    }

    public int getRowNumber() {
        return sourceNode.getAlgorithmRow().getRowNumber();
    }
}
