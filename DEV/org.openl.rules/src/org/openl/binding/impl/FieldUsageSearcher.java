package org.openl.binding.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.meta.IMetaInfo;
import org.openl.rules.calc.IOriginalDeclaredClassesOpenField;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public final class FieldUsageSearcher {

    private static final int MAX_DESCRIPTION_CLASS_NUMBER = 3;

    private FieldUsageSearcher() {
    }

    public static void findAllFields(List<NodeUsage> fields,
            IBoundNode boundNode,
            String sourceString,
            int startPosition) {
        if (boundNode == null) {
            return;
        }
        IBoundNode targetNode = boundNode.getTargetNode();
        findAllFields(fields, targetNode, sourceString, startPosition);
        IBoundNode[] children = boundNode.getChildren();
        if (children != null) {
            for (IBoundNode child : children) {
                findAllFields(fields, child, sourceString, startPosition);
            }
        }
        if (boundNode instanceof FieldBoundNode) {

            FieldBoundNode fieldNode = (FieldBoundNode) boundNode;
            IOpenField boundField = fieldNode.getBoundField();
            IOpenClass type = boundField.getDeclaringClass();
            if ((type == null || type == NullOpenClass.the) && targetNode != null) {
                type = targetNode.getType();
            }
            if (type == null) {
                type = boundField.getType();
            }
            if (type == null) {
                return;
            }

            TextInfo tableHeaderText = new TextInfo(sourceString);
            ISyntaxNode syntaxNode = boundNode.getSyntaxNode();
            String description;
            String uri = null;
            if (boundField instanceof IOriginalDeclaredClassesOpenField) {
                IOriginalDeclaredClassesOpenField combinedOpenField = (IOriginalDeclaredClassesOpenField) boundField;
                IOpenClass[] declaredClasses = combinedOpenField.getDeclaredClasses();
                String classNames = Arrays.stream(declaredClasses)
                    .map(c -> c.getName().replaceAll(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX, ""))
                    .limit(MAX_DESCRIPTION_CLASS_NUMBER)
                    .collect(Collectors.joining(","));
                if (declaredClasses.length > MAX_DESCRIPTION_CLASS_NUMBER) {
                    classNames += "...(" + (declaredClasses.length - MAX_DESCRIPTION_CLASS_NUMBER) + ")more";
                }
                String prefix = declaredClasses.length > 1 ? "Spreadsheets: " : "Spreadsheet ";
                description = prefix + classNames + "\n" + MethodUtil.printType(boundField.getType()) + " " + boundField
                    .getName();
                syntaxNode = getIdentifierSyntaxNode(syntaxNode);
                IMetaInfo metaInfo = type.getMetaInfo();
                if (metaInfo != null) {
                    uri = metaInfo.getSourceUrl();
                } else {
                    IMetaInfo mi = boundField.getType().getMetaInfo();
                    if (mi != null) {
                        uri = mi.getSourceUrl();
                    }
                }
            } else if (boundField instanceof NodeDescriptionHolder) {
                NodeDescriptionHolder nodeDescriptionHolder = (NodeDescriptionHolder) boundField;
                description = nodeDescriptionHolder.getDescription();
                syntaxNode = getIdentifierSyntaxNode(syntaxNode);
            } else if (type instanceof XlsModuleOpenClass && boundField instanceof DataOpenField) {
                final ITable foreignTable = ((DataOpenField) boundField).getTable();
                TableSyntaxNode tableSyntaxNode = foreignTable.getTableSyntaxNode();
                description = tableSyntaxNode.getHeaderLineValue().getValue();
                uri = tableSyntaxNode.getUri();
            } else if (type instanceof XlsModuleOpenClass && boundField instanceof ConstantOpenField) {
                ConstantOpenField constantOpenField = (ConstantOpenField) boundField;
                description = MethodUtil.printType(boundField.getType()) + " " + boundField
                    .getName() + " = " + constantOpenField.getValueAsString();
                uri = constantOpenField.getMemberMetaInfo().getSourceUrl();
            } else {
                IMetaInfo metaInfo = type.getMetaInfo();
                while (metaInfo == null && type.isArray()) {
                    type = type.getComponentClass();
                    metaInfo = type.getMetaInfo();
                }
                syntaxNode = getIdentifierSyntaxNode(syntaxNode);
                description = MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
                if (metaInfo != null) {
                    uri = metaInfo.getSourceUrl();
                    description = metaInfo.getDisplayName(IMetaInfo.REGULAR) + "\n" + description;
                } else if (type != NullOpenClass.the) {
                    description = MethodUtil.printType(type) + "\n" + description;
                } else {
                    IMetaInfo mi = boundField.getType().getMetaInfo();
                    if (mi != null) {
                        uri = mi.getSourceUrl();
                    }
                }
            }
            ILocation typeLocation = syntaxNode.getSourceLocation();
            if (typeLocation != null) {
                int start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
                int end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText);
                fields.add(new SimpleNodeUsage(start, end, description, uri, NodeType.FIELD));
            }
        }
    }

    private static ISyntaxNode getIdentifierSyntaxNode(ISyntaxNode syntaxNode) {
        if ("function".equals(syntaxNode.getType())) {
            syntaxNode = syntaxNode.getChild(syntaxNode.getNumberOfChildren() - 1);
        }
        return syntaxNode;
    }

}
