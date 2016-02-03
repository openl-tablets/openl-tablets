package org.openl.rules.tbasic.runtime.debug;

import java.util.HashMap;

import org.openl.rules.tbasic.compile.AlgorithmOperationSource;
import org.openl.vm.trace.SimpleTracerObject;

public class TBasicOperationTraceObject extends SimpleTracerObject {

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

    @Override
    public String getUri() {
        return sourceCode.getSourceUri();
    }

    @SuppressWarnings("unchecked")
    public void setFieldValues(HashMap<String, Object> fieldValues) {
        this.fieldValues = (HashMap<String, Object>) fieldValues.clone();
    }
}
