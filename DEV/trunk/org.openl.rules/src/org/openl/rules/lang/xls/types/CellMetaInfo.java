package org.openl.rules.lang.xls.types;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openl.binding.impl.MethodUsagesSearcher.MethodUsage;
import org.openl.rules.table.ICell;
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
    private List<MethodUsage> usedMethods;

    public CellMetaInfo(Type type, String paramName, IOpenClass domain, boolean multiValue) {
        this(type, paramName, domain, multiValue, null);
    }
    
    public CellMetaInfo(Type type, String paramName, IOpenClass domain, boolean multiValue, List<MethodUsage> usedMethods) {
        this.type = type;
        this.domain = domain;
        this.paramName = paramName;
        this.setMultiValue(multiValue);
        this.usedMethods = usedMethods;
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

    public List<MethodUsage> getUsedMethods() {
        return usedMethods;
    }

    public void setUsedMethods(List<MethodUsage> usedMethods) {
        this.usedMethods = usedMethods;
    }
    
    public boolean hasMethodUsagesInCell() {
        return !CollectionUtils.isEmpty(getUsedMethods());
    }
    
    public static boolean isCellContainsMethodUsages(ICell cell){
        return cell.getMetaInfo() != null && cell.getMetaInfo().hasMethodUsagesInCell();
    }
}
