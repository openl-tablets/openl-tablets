package org.openl.rules.lang.xls.types;

import org.openl.types.IOpenClass;

public class CellMetaInfo {

    public static enum Type {
        TABLE_HEADER,
        TABLE_PROPERTIES,
        DT_CA_HEADER,
        DT_CA_CODE,
        DT_CA_DISPLAY,
        DT_DATA_CELL
    };

    private Type type;
    private IOpenClass domain;
    private String paramName;
    private boolean multiValue;

    public CellMetaInfo(Type type, String paramName, IOpenClass domain, boolean multiValue) {
        this.type = type;
        this.domain = domain;
        this.paramName = paramName;
        this.setMultiValue(multiValue);
    }

    public IOpenClass getDataType() {
        return domain;
    }

    public String getParamName() {
        return paramName;
    }

    public Type getType() {
        return type;
    }

    public void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }

    public boolean isMultiValue() {
        return multiValue;
    }

}
