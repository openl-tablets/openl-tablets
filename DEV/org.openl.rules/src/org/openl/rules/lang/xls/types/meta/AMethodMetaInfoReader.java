package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public abstract class AMethodMetaInfoReader<T extends AMethodBasedNode> extends BaseMetaInfoReader<T> {

    public AMethodMetaInfoReader(T boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        TableSyntaxNode syntaxNode = getTableSyntaxNode();
        OpenMethodHeader tableHeader = (OpenMethodHeader) getBoundNode().getHeader();

        List<NodeUsage> nodeUsages = new ArrayList<>();
        ICell cell = syntaxNode.getGridTable().getCell(0, 0);
        TextInfo tableHeaderText = new TextInfo(cell.getStringValue());

        int startPosition = getBoundNode().getSignatureStartIndex();
        // Link to return type
        IOpenClass type = tableHeader.getType();
        IMetaInfo metaInfo = type.getMetaInfo();
        while (metaInfo == null && type.isArray()) {
            type = type.getComponentClass();
            metaInfo = type.getMetaInfo();
        }

        ILocation typeLocation = tableHeader.getTypeLocation();
        if (metaInfo != null && typeLocation != null) {
            int start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
            int end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText);
            nodeUsages.add(new SimpleNodeUsage(start,
                end,
                metaInfo.getDisplayName(INamedThing.SHORT),
                metaInfo.getSourceUrl(),
                NodeType.DATATYPE));
        }

        // Link to input parameters
        ILocation[] paramTypeLocations = tableHeader.getParamTypeLocations();
        if (paramTypeLocations != null) {
            for (int i = 0; i < tableHeader.getSignature().getNumberOfParameters(); i++) {
                IOpenClass parameterType = tableHeader.getSignature().getParameterType(i);
                metaInfo = parameterType.getMetaInfo();
                while (metaInfo == null && parameterType.isArray()) {
                    parameterType = parameterType.getComponentClass();
                    metaInfo = parameterType.getMetaInfo();
                }

                if (metaInfo != null) {
                    ILocation sourceLocation = paramTypeLocations[i];
                    int start = startPosition + sourceLocation.getStart().getAbsolutePosition(tableHeaderText);
                    int end = startPosition + sourceLocation.getEnd().getAbsolutePosition(tableHeaderText);
                    nodeUsages.add(new SimpleNodeUsage(start,
                        end,
                        metaInfo.getDisplayName(INamedThing.SHORT),
                        metaInfo.getSourceUrl(),
                        NodeType.DATATYPE));
                }
            }
        }

        if (CollectionUtils.isNotEmpty(nodeUsages)) {
            return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
        }

        return null;
    }
}
