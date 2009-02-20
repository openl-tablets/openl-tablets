/**
 * 
 */
package org.openl.rules.tbasic.runtime.debug;

import java.util.HashMap;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.runtime.Result;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

/**
 * @author User
 * 
 */
public class TBasicOperationTraceObject extends ATBasicTraceObjectLeaf {

    private Result result;
    private HashMap<String, Object> fieldValues;

    public TBasicOperationTraceObject(Object tracedObject) {
        super(tracedObject);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.vm.ITracerObject.SimpleTracerObject#getUri()
     */
    @Override
    public String getUri() {
        RuntimeOperation operation = (RuntimeOperation) getTraceObject();
        String operationUri = operation.getSourceCode().getSourceUri();

        return operationUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.util.ITreeElement#getType()
     */
    public String getType() {
        return "tbasicAlgorithmOperation";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.base.INamedThing#getDisplayName(int)
     */
    public String getDisplayName(int mode) {
        RuntimeOperation operation = (RuntimeOperation) getTraceObject();
        String operationName = operation.getSourceCode().getOperationName();
        int operationRow = operation.getSourceCode().getRowNumber();

        String displayName = String.format("%s in row %d", operationName, operationRow);

        return "Step " + displayName;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    /**
     * @return the result
     */
    public Result getResult() {
        return result;
    }

    @SuppressWarnings("unchecked")
    public void setFieldValues(HashMap<String, Object> fieldValues) {
        this.fieldValues = (HashMap<String, Object>) fieldValues.clone();
    }

    /**
     * @return the fieldValues
     */
    public HashMap<String, Object> getFieldValues() {
        return fieldValues;
    }

    public IGridRegion getGridRegion() {
        RuntimeOperation operation = (RuntimeOperation) getTraceObject();
        return operation.getSourceCode().getGridRegion();
    }

}
