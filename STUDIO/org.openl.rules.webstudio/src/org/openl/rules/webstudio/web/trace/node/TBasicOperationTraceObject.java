package org.openl.rules.webstudio.web.trace.node;

import java.util.HashMap;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.compile.AlgorithmOperationSource;

public class TBasicOperationTraceObject extends SimpleTracerObject {

    private HashMap<String, Object> fieldValues;
    private String nameForDebug;
    private String uri;
    private IGridRegion gridRegion;
    private String operationName;
    private int operationRow;


    TBasicOperationTraceObject(AlgorithmOperationSource sourceCode, String nameForDebug) {
        super("tbasicOperation");
        this.nameForDebug = nameForDebug;
        this.gridRegion = sourceCode.getGridRegion();
        this.operationName = sourceCode.getOperationName();
        this.operationRow = sourceCode.getRowNumber();
        this.uri = sourceCode.getSourceUri();
    }

    TBasicOperationTraceObject(String nameForDebug, IGridRegion gridRegion, String operationName, int operationRow, String uri, HashMap<String, Object> fieldValues) {
        super("tbasicOperation");
        this.nameForDebug = nameForDebug;
        this.gridRegion = gridRegion;
        this.operationName = operationName;
        this.operationRow = operationRow;
        this.uri = uri;
        this.fieldValues = fieldValues;
    }

    public String getNameForDebug() {
        return nameForDebug;
    }

    public HashMap<String, Object> getFieldValues() {
        return fieldValues;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @SuppressWarnings("unchecked")
    public void setFieldValues(HashMap<String, Object> fieldValues) {
        this.fieldValues = (HashMap<String, Object>) fieldValues.clone();
    }

    public IGridRegion getGridRegion() {
        return gridRegion;
    }

    public String getOperationName() {
        return operationName;
    }

    public int getOperationRow() {
        return operationRow;
    }
}
