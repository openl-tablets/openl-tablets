package org.openl.rules.tbasic.runtime.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.compile.AlgorithmOperationSource;

public class TBasicOperationTraceObject extends ATBasicTraceObjectLeaf {

    private HashMap<String, Object> fieldValues;
    private AlgorithmOperationSource sourceCode;
    private String nameForDebug;

    public TBasicOperationTraceObject(AlgorithmOperationSource sourceCode, String nameForDebug) {
        super("tbasicOperation");
        this.sourceCode = sourceCode;
        this.nameForDebug = nameForDebug;
    }

    public AlgorithmOperationSource getSourceCode() {
        return sourceCode;
    }

    public String getNameForDebug() {
        return nameForDebug;
    }

    public HashMap<String, Object> getFieldValues() {
        return fieldValues;
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        regions.add(sourceCode.getGridRegion());
        return regions;
    }

    @Override
    public String getUri() {
        return sourceCode.getSourceUri();
    }

    @SuppressWarnings("unchecked")
    public void setFieldValues(HashMap<String, Object> fieldValues) {
        this.fieldValues = (HashMap<String, Object>) fieldValues.clone();
    }
}
