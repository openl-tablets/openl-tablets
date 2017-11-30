package org.openl.binding.impl;

import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.meta.IMetaInfo;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public final class FieldUsageSearcher {
    private FieldUsageSearcher() {
    }

    public static void findAllFields(List<NodeUsage> fields,
            IBoundNode boundNode, String sourceString,
            int startPosition) {
        if (boundNode instanceof FieldBoundNode) {
            if (boundNode.getTargetNode() != null) {
                findAllFields(fields, boundNode.getTargetNode(), sourceString, startPosition);
            }

            TextInfo tableHeaderText = new TextInfo(sourceString);
            IOpenField boundField = ((FieldBoundNode) boundNode).getBoundField();
            IOpenClass type = boundField.getDeclaringClass();
            if (type == null) {
                return;
            }

            if (type instanceof XlsModuleOpenClass) {
                if (boundField instanceof DataOpenField) {
                    fields.add(createDataTableFieldUsage(boundNode,
                            startPosition,
                            tableHeaderText,
                            (DataOpenField) boundField));
                }
            } else {
                SimpleNodeUsage simpleNodeUsage = createFieldOfDatatype(boundNode.getSyntaxNode(),
                        startPosition,
                        tableHeaderText,
                        type,
                        boundField);
                if (simpleNodeUsage != null) {
                    fields.add(simpleNodeUsage);
                }
            }
        } else if (boundNode instanceof IndexNode) {
            findAllFields(fields, boundNode.getTargetNode(), sourceString, startPosition);
        } else {
            if (boundNode.getChildren() == null) {
                return;
            }
            for (IBoundNode child : boundNode.getChildren()) {
                findAllFields(fields, child, sourceString, startPosition);
            }
        }
    }

    public static SimpleNodeUsage createFieldOfDatatype(ISyntaxNode syntaxNode,
            int startPosition,
            TextInfo tableHeaderText, IOpenClass type, IOpenField field) {
        IMetaInfo metaInfo = type.getMetaInfo();
        while (metaInfo == null && type.isArray()) {
            type = type.getComponentClass();
            metaInfo = type.getMetaInfo();
        }
        if (!(syntaxNode instanceof IdentifierNode)) {
            if ("function".equals(syntaxNode.getType())) {
                syntaxNode = syntaxNode.getChild(syntaxNode.getNumberOfChildren() - 1);
            }
        }
        ILocation typeLocation = syntaxNode.getSourceLocation();
        SimpleNodeUsage simpleNodeUsage = null;
        if (metaInfo != null && typeLocation != null) {
            int start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
            int end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText);
            String description = MethodUtil.printType(type) + "\n" +
                    MethodUtil.printType(field.getType()) + " " + field.getName();
            simpleNodeUsage = new SimpleNodeUsage(start,
                    end,
                    description,
                    metaInfo.getSourceUrl(),
                    NodeType.FIELD);
        }
        return simpleNodeUsage;
    }

    public static TableUsage createDataTableFieldUsage(IBoundNode boundNode,
            int startPosition,
            TextInfo tableHeaderText,
            DataOpenField boundField) {
        ILocation typeLocation = boundNode.getSyntaxNode().getSourceLocation();
        int start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
        int end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText);
        return new TableUsage(boundField.getTable(), start, end, NodeType.FIELD);
    }
}
