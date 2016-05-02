package org.openl.rules.lang.xls.types;

import java.util.List;

import org.openl.binding.impl.NodeUsage;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;

public class CellMetaInfo {

    public static enum Type {
        TABLE_HEADER,
        TABLE_PROPERTIES,
        DT_CA_HEADER,
        DT_CA_CODE,
        DT_CA_DISPLAY,
        DT_DATA_CELL
    }

    private Type type;
    private IOpenClass domain;
    private String paramName;
    private boolean multiValue;
    private List<? extends NodeUsage> usedNodes;

    public CellMetaInfo(Type type, String paramName, IOpenClass domain, boolean multiValue) {
        this(type, paramName, domain, multiValue, null);
    }
    
    public CellMetaInfo(Type type, String paramName, IOpenClass domain, boolean multiValue, List<? extends NodeUsage> usedNodes) {
        this.type = type;
        this.domain = domain;
        this.paramName = paramName;
        this.setMultiValue(multiValue);
        this.usedNodes = usedNodes;
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

    public List<? extends NodeUsage> getUsedNodes() {
        return usedNodes;
    }

    public void setUsedNodes(List<? extends NodeUsage> usedNodes) {
        this.usedNodes = usedNodes;
    }
    
    public boolean hasNodeUsagesInCell() {
        return CollectionUtils.isNotEmpty(getUsedNodes());
    }

    public static boolean isCellContainsNodeUsages(ICell cell){
        return cell.getMetaInfo() != null && cell.getMetaInfo().hasNodeUsagesInCell();
    }
}
