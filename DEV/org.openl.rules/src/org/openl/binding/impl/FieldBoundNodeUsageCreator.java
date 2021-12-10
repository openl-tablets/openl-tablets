package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openl.binding.IBoundNode;
import org.openl.binding.MethodUtil;
import org.openl.dependency.DependencyType;
import org.openl.dependency.DependencyVar;
import org.openl.meta.IMetaInfo;
import org.openl.rules.calc.UnifiedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
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
            if (Arrays.stream(declaredClasses).allMatch(e -> e instanceof CustomSpreadsheetResultOpenClass)) {
                Collection<CustomSpreadsheetResultOpenClass> customSpreadsheetResultOpenClasses = Arrays
                    .stream(declaredClasses)
                    .map(CustomSpreadsheetResultOpenClass.class::cast)
                    .flatMap(
                        e -> e instanceof UnifiedSpreadsheetResultOpenClass ? ((UnifiedSpreadsheetResultOpenClass) e)
                            .getUnifiedTypes()
                            .stream() : Stream.of(e))
                    .collect(Collectors.toList());
                Map<IOpenClass, List<CustomSpreadsheetResultOpenClass>> groupedByTypes = customSpreadsheetResultOpenClasses
                    .stream()
                    .filter(e -> e.getField(boundField.getName()) != null)
                    .collect(Collectors.groupingBy(c -> c.getField(boundField.getName()).getType()));
                StringBuilder classNames = new StringBuilder();
                if (groupedByTypes.keySet().size() > 1) {
                    for (Map.Entry<IOpenClass, List<CustomSpreadsheetResultOpenClass>> e : groupedByTypes.entrySet()) {
                        classNames.append("\n").append(MethodUtil.printType(e.getKey())).append(" in ");
                        classNames.append(concatenateSpreadsheetResultTables(new ArrayList<>(e.getValue())));
                    }
                }
                description = MethodUtil.printType(boundField.getDeclaringClass()) + classNames + "\n" + MethodUtil
                    .printType(boundField.getType()) + " " + boundField.getName();
            } else {
                description = MethodUtil.printType(boundField.getDeclaringClass()) + "\n" + MethodUtil
                    .printType(boundField.getType()) + " " + boundField.getName();
            }
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

    private static String concatenateSpreadsheetResultTables(Collection<CustomSpreadsheetResultOpenClass> types) {
        StringBuilder sb = new StringBuilder();
        boolean stringLengthExceeded = false;
        int concatenated = 0;
        for (CustomSpreadsheetResultOpenClass c : types.size() > 3 ? new ArrayList<>(types).subList(0, 3) : types) {
            concatenated++;
            StringBuilder sb1 = new StringBuilder();
            if (c instanceof UnifiedSpreadsheetResultOpenClass) {
                for (CustomSpreadsheetResultOpenClass t : ((UnifiedSpreadsheetResultOpenClass) c).getUnifiedTypes()) {
                    if (sb1.length() > 0) {
                        sb1.append(", ");
                    }
                    sb1.append(t.getName().substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()));
                }
            } else if (c != null) {
                if (sb1.length() > 0) {
                    sb1.append(", ");
                }
                sb1.append(c.getName().substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()));
            } else {
                throw new IllegalStateException();
            }
            if (sb.length() + sb1.length() > MAX_DESCRIPTION_CLASS_LENGTH) {
                stringLengthExceeded = true;
                break;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(sb1);
            concatenated++;
        }
        int more = types.size() - concatenated;
        if (types.size() > MAX_DESCRIPTION_CLASS_NUMBER || stringLengthExceeded) {
            sb.append("...(").append(more).append(") more");
        }
        return sb.toString();
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
