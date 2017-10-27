package org.openl.rules.dt.algorithm;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.text.ILocation;

public class FailOnMissException extends OpenLRuntimeException {
    
    private static final long serialVersionUID = -4344185808917149412L;
    
    private DecisionTable decisionTable; 
    private Object[] invocationParameters;
    
    public FailOnMissException(String message, DecisionTable decisionTable, Object[] invocationParameters) {
        super(message);
        
        this.decisionTable = decisionTable;
        this.invocationParameters = invocationParameters.clone();
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public Object[] getInvocationParameters() {
        return invocationParameters;
    }

    @Override
    public ILocation getLocation() {
    
        if (decisionTable != null) {
            TableSyntaxNode syntaxNode = decisionTable.getSyntaxNode();
            if (syntaxNode != null) {
                return syntaxNode.getLocation();
            }
        }
        
        return null;
    }

    @Override
    public IOpenSourceCodeModule getSourceModule() {

        if (decisionTable != null) {
            TableSyntaxNode syntaxNode = decisionTable.getSyntaxNode();
            if (syntaxNode != null) {
                return syntaxNode.getXlsSheetSourceCodeModule();
            }
        }
        
        return null;
    }
    
}
