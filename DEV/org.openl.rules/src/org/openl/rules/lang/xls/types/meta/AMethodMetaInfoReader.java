package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;

import org.openl.base.INamedThing;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.text.TextInfo;

public abstract class AMethodMetaInfoReader<T extends AMethodBasedNode> extends BaseMetaInfoReader<T> {

    public AMethodMetaInfoReader(T boundNode) {
        super(boundNode);
    }

    @Override
    protected TableSyntaxNode getTableSyntaxNode() {
        return getBoundNode().getTableSyntaxNode();
    }

    protected String getAdditionalMetaInfoForTableReturnType() {
        return null;
    }

    @Override
    protected CellMetaInfo getHeaderMetaInfo() {
        var syntaxNode = getTableSyntaxNode();
        var tableHeader = (OpenMethodHeader) getBoundNode().getHeader();

        var nodeUsages = new ArrayList<NodeUsage>();
        var cell = syntaxNode.getGridTable().getCell(0, 0);
        var tableHeaderText = new TextInfo(cell.getStringValue());

        var startPosition = getBoundNode().getSignatureStartIndex();
        // Link to return type
        var type = tableHeader.getType();
        var metaInfo = type.getMetaInfo();
        while (metaInfo == null && type.isArray()) {
            type = type.getComponentClass();
            metaInfo = type.getMetaInfo();
        }

        var typeLocation = tableHeader.getTypeLocation();
        if (metaInfo != null && typeLocation != null) {
            var start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
            var end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText) + 1; // 1 - is because typeLocation returns 'end' inclusively
            nodeUsages.add(
                    new SimpleNodeUsage(
                            start,
                            end,
                            metaInfo.getDisplayName(INamedThing.SHORT)
                                    + (StringUtils.isEmpty(getAdditionalMetaInfoForTableReturnType())
                                    ? ""
                                    : ("\n" + getAdditionalMetaInfoForTableReturnType())),
                            metaInfo.getSourceUrl(),
                            type,
                            NodeType.DATATYPE)
            );
        }

        // Link to input parameters
        var paramTypeLocations = tableHeader.getParamTypeLocations();
        if (paramTypeLocations != null) {
            for (var i = 0; i < tableHeader.getSignature().getNumberOfParameters(); i++) {
                var parameterType = tableHeader.getSignature().getParameterType(i);
                metaInfo = parameterType.getMetaInfo();
                while (metaInfo == null && parameterType.isArray()) {
                    parameterType = parameterType.getComponentClass();
                    metaInfo = parameterType.getMetaInfo();
                }

                if (metaInfo != null) {
                    var sourceLocation = paramTypeLocations[i];
                    var start = startPosition + sourceLocation.getStart().getAbsolutePosition(tableHeaderText);
                    var end = startPosition + sourceLocation.getEnd().getAbsolutePosition(tableHeaderText) + 1; // 1 - is because location returns 'end' inclusively
                    nodeUsages.add(new SimpleNodeUsage(start,
                            end,
                            metaInfo.getDisplayName(INamedThing.SHORT),
                            metaInfo.getSourceUrl(),
                            parameterType,
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
