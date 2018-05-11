package org.openl.rules.lang.xls.types;

import java.util.List;

import org.openl.binding.impl.NodeUsage;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;

public class CellMetaInfo {
    private IOpenClass domain;
    private boolean multiValue;
    private List<? extends NodeUsage> usedNodes;

    public CellMetaInfo(IOpenClass domain, boolean multiValue) {
        this(domain, multiValue, null);
    }
    
    public CellMetaInfo(IOpenClass domain, boolean multiValue, List<? extends NodeUsage> usedNodes) {
        this.domain = domain;
        this.multiValue = multiValue;
        this.usedNodes = usedNodes;
    }

    public IOpenClass getDataType() {
        return domain;
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
    
    private boolean hasNodeUsagesInCell() {
        return CollectionUtils.isNotEmpty(getUsedNodes());
    }

    public static boolean isCellContainsNodeUsages(CellMetaInfo metaInfo) {
        return metaInfo != null && metaInfo.hasNodeUsagesInCell();
    }
}
