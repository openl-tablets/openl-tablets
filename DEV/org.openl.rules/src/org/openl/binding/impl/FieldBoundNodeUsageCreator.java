package org.openl.binding.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.dependency.DependencyType;
import org.openl.dependency.DependencyVar;
import org.openl.meta.IMetaInfo;
import org.openl.rules.calc.IOriginalDeclaredClassesOpenField;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.dt.DTColumnsDefinitionField;
import org.openl.rules.dt.data.ConditionOrActionDirectParameterField;
import org.openl.rules.dt.data.ConditionOrActionParameterField;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

final class FieldBoundNodeUsageCreator implements NodeUsageCreator {

    private static final int MAX_DESCRIPTION_CLASS_NUMBER = 3;
    private static final int MAX_DESCRIPTION_CLASS_LENGTH = 50;

    private FieldBoundNodeUsageCreator() {
    }

    @Override
    public boolean accept(IBoundNode boundNode) {
        return boundNode instanceof FieldBoundNode;
    }

    @Override
    public Optional<NodeUsage> create(IBoundNode boundNode, String sourceString, int startPosition) {
        IBoundNode targetNode = boundNode.getTargetNode();
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
            return Optional.empty();
        }

        TextInfo tableHeaderText = new TextInfo(sourceString);
        ISyntaxNode syntaxNode = boundNode.getSyntaxNode();
        String description;
        String uri = null;
        if (boundField instanceof IOriginalDeclaredClassesOpenField) {
            IOriginalDeclaredClassesOpenField combinedOpenField = (IOriginalDeclaredClassesOpenField) boundField;
            IOpenClass[] declaredClasses = combinedOpenField.getDeclaringClasses();
            Map<IOpenClass, List<IOpenClass>> types = Arrays.stream(declaredClasses)
                .collect(Collectors.groupingBy(c -> c.getField(boundField.getName(), false).getType()));
            StringBuilder classNames = new StringBuilder();
            if (types.keySet().size() > 1) {
                for (IOpenClass iOpenClass : types.keySet()) {
                    classNames.append("\n").append(MethodUtil.printType(iOpenClass)).append(" in ");
                    classNames.append(configureClassNames(types.get(iOpenClass)));
                }
            } else if (types.keySet().size() == 1) {
                classNames = new StringBuilder(configureClassNames(types.values().iterator().next()));
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
        } else if (boundField instanceof DependencyVar) {
            DependencyVar dependencyVar = (DependencyVar) boundField;
            description = (DependencyType.PROJECT
                .equals(dependencyVar.getDependencyType()) ? "Project '" : "Module '") + dependencyVar.getName() + "'";
            if (DependencyType.MODULE.equals(dependencyVar.getDependencyType())) {
                uri = dependencyVar.getType().getMetaInfo().getSourceUrl();
            }
        } else if (boundField instanceof ConditionOrActionParameterField) {
            ConditionOrActionParameterField conditionOrActionParameterField = (ConditionOrActionParameterField) boundField;
            description = "Parameter of " + conditionOrActionParameterField.getConditionOrAction()
                .getName() + "\n" + MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
        } else if (boundField instanceof ConditionOrActionDirectParameterField) {
            ConditionOrActionDirectParameterField conditionOrActionDirectParameterField = (ConditionOrActionDirectParameterField) boundField;
            description = "Parameter of " + conditionOrActionDirectParameterField.getConditionOrAction()
                .getName() + "\n" + MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
        } else if (boundField instanceof DTColumnsDefinitionField) {
            DTColumnsDefinitionField dtColumnsDefinitionField = (DTColumnsDefinitionField) boundField;
            DTColumnsDefinition dtColumnsDefinition = dtColumnsDefinitionField.getDtColumnsDefinition();
            String columnType;
            if (dtColumnsDefinition.isCondition()) {
                columnType = "condition";
            } else {
                columnType = dtColumnsDefinition.isAction() ? "action" : "return";
            }
            description = "External " + columnType + " parameter" + "\n" + MethodUtil
                .printType(boundField.getType()) + " " + boundField.getName();
            uri = dtColumnsDefinition.getUri();
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
            } else if (type != NullOpenClass.the && !(type instanceof DecisionTableDataType)) {
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
            return Optional.of(new SimpleNodeUsage(start, end, description, uri, NodeType.FIELD));
        }
        return Optional.empty();
    }

    private static String configureClassNames(List<IOpenClass> classes) {
        List<IOpenClass> iOpenClasses;
        if (classes.size() > 3) {
            iOpenClasses = classes.subList(0, 3);
        } else {
            iOpenClasses = classes;
        }
        String classNames = iOpenClasses.stream()
            .map(c -> c.getName().replaceAll(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX, ""))
            .collect(Collectors.joining(","));
        int classRemoved = 0;
        if (classNames.length() > MAX_DESCRIPTION_CLASS_LENGTH && classes.size() > 1) {
            for (int i = classes.size() - 1; i != 0 && classNames.length() > MAX_DESCRIPTION_CLASS_LENGTH; i--) {
                classRemoved++;
                classNames = classNames.replaceAll(
                    "," + classes.get(i).getName().replaceAll(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX, ""),
                    "");
            }
        }
        if (classes.size() > MAX_DESCRIPTION_CLASS_NUMBER || classRemoved > 0) {
            int more = classRemoved;
            if (classes.size() > MAX_DESCRIPTION_CLASS_NUMBER) {
                more += classes.size() - MAX_DESCRIPTION_CLASS_NUMBER;
            }
            classNames += "...(" + more + ")more";
        }
        return classNames;
    }

    private static ISyntaxNode getIdentifierSyntaxNode(ISyntaxNode syntaxNode) {
        if ("function".equals(syntaxNode.getType())) {
            syntaxNode = syntaxNode.getChild(syntaxNode.getNumberOfChildren() - 1);
        }
        return syntaxNode;
    }

    private static class Holder {
        private static final FieldBoundNodeUsageCreator INSTANCE = new FieldBoundNodeUsageCreator();
    }

    public static FieldBoundNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }

}
