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
import org.openl.rules.calc.CombinedSpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.IOriginalDeclaredClassesOpenField;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.dt.DTColumnsDefinitionField;
import org.openl.rules.dt.data.ConditionOrActionDirectParameterField;
import org.openl.rules.dt.data.ConditionOrActionParameterField;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
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
        var targetNode = boundNode.getTargetNode();
        var fieldNode = (FieldBoundNode) boundNode;
        var boundField = fieldNode.getBoundField();
        var type = boundField.getDeclaringClass();
        if ((type == null || type == NullOpenClass.the) && targetNode != null) {
            type = targetNode.getType();
        }
        if (type == null) {
            type = boundField.getType();
        }
        if (type == null) {
            return Optional.empty();
        }

        var tableHeaderText = new TextInfo(sourceString);
        var usage = describe(boundField, type, boundNode.getSyntaxNode());
        var typeLocation = usage.syntaxNode().getSourceLocation();
        if (typeLocation != null) {
            var start = startPosition + typeLocation.getStart().getAbsolutePosition(tableHeaderText);
            var end = startPosition + typeLocation.getEnd().getAbsolutePosition(tableHeaderText) + 1; // 1 - is because typeLocation returns 'end' inclusively
            return Optional.of(new SimpleNodeUsage(start, end, usage.description(), usage.uri(), NodeType.FIELD));
        }
        return Optional.empty();
    }

    /**
     * Describes the field usage: the node it covers, the hover text and the source of the field declaration.
     *
     * @param type the type the field is accessed from
     */
    private static FieldUsage describe(IOpenField boundField, IOpenClass type, ISyntaxNode syntaxNode) {
        if (boundField instanceof IOriginalDeclaredClassesOpenField combinedOpenField) {
            var description = describeCombinedField(combinedOpenField, boundField);
            return new FieldUsage(getIdentifierSyntaxNode(syntaxNode), description, getSourceUrl(type, boundField));
        }
        if (boundField instanceof NodeDescriptionHolder nodeDescriptionHolder) {
            return new FieldUsage(getIdentifierSyntaxNode(syntaxNode), nodeDescriptionHolder.getDescription(), null);
        }
        if (type instanceof XlsModuleOpenClass && boundField instanceof DataOpenField field) {
            final var foreignTable = field.getTable();
            var tableSyntaxNode = foreignTable.getTableSyntaxNode();
            return new FieldUsage(syntaxNode,
                    tableSyntaxNode.getHeaderLineValue().getValue(),
                    tableSyntaxNode.getUri());
        }
        if (type instanceof XlsModuleOpenClass && boundField instanceof ConstantOpenField constantOpenField) {
            var description = MethodUtil.printType(boundField.getType()) + " " + boundField
                    .getName() + " = " + constantOpenField.getValueAsString();
            return new FieldUsage(syntaxNode, description, constantOpenField.getMemberMetaInfo().getSourceUrl());
        }
        if (boundField instanceof DependencyVar dependencyVar) {
            return describeDependency(dependencyVar, syntaxNode);
        }
        if (boundField instanceof ConditionOrActionParameterField conditionOrActionParameterField) {
            var description = "Parameter of " + conditionOrActionParameterField.getConditionOrAction()
                    .getName() + "\n" + MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
            return new FieldUsage(syntaxNode, description, null);
        }
        if (boundField instanceof ConditionOrActionDirectParameterField conditionOrActionDirectParameterField) {
            var description = "Parameter of " + conditionOrActionDirectParameterField.getConditionOrAction()
                    .getName() + "\n" + MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
            return new FieldUsage(syntaxNode, description, null);
        }
        if (boundField instanceof DTColumnsDefinitionField dtColumnsDefinitionField) {
            var dtColumnsDefinition = dtColumnsDefinitionField.getDtColumnsDefinition();
            var description = "External " + getColumnType(dtColumnsDefinition) + " parameter" + "\n" + MethodUtil
                    .printType(boundField.getType()) + " " + boundField.getName();
            return new FieldUsage(syntaxNode, description, dtColumnsDefinition.getUri());
        }
        return describeByType(boundField, type, syntaxNode);
    }

    private static String describeCombinedField(IOriginalDeclaredClassesOpenField combinedOpenField,
                                                IOpenField boundField) {
        var declaredClasses = combinedOpenField.getDeclaringClasses();
        if (Arrays.stream(declaredClasses).allMatch(CustomSpreadsheetResultOpenClass.class::isInstance)) {
            var customSpreadsheetResultOpenClasses = Arrays
                    .stream(declaredClasses)
                    .map(CustomSpreadsheetResultOpenClass.class::cast)
                    .flatMap(
                            e -> e instanceof CombinedSpreadsheetResultOpenClass csroc ? csroc
                                    .getCombinedTypes()
                                    .stream() : Stream.of(e))
                    .toList();
            var groupedByTypes = customSpreadsheetResultOpenClasses
                    .stream()
                    .filter(e -> e.getField(boundField.getName()) != null)
                    .collect(Collectors.groupingBy(c -> c.getField(boundField.getName()).getType()));
            var classNames = new StringBuilder();
            if (groupedByTypes.keySet().size() > 1) {
                for (Map.Entry<IOpenClass, List<CustomSpreadsheetResultOpenClass>> e : groupedByTypes.entrySet()) {
                    classNames.append("\n").append(MethodUtil.printType(e.getKey())).append(" in ");
                    classNames.append(concatenateSpreadsheetResultTables(new ArrayList<>(e.getValue())));
                }
            }
            return MethodUtil.printType(boundField.getDeclaringClass()) + classNames + "\n" + MethodUtil
                    .printType(boundField.getType()) + " " + boundField.getName();
        }
        return MethodUtil.printType(boundField.getDeclaringClass()) + "\n" + MethodUtil
                .printType(boundField.getType()) + " " + boundField.getName();
    }

    private static FieldUsage describeDependency(DependencyVar dependencyVar, ISyntaxNode syntaxNode) {
        var description = (DependencyType.PROJECT
                .equals(dependencyVar.getDependencyType()) ? "Project '" : "Module '") + dependencyVar.getName() + "'";
        String uri = null;
        if (DependencyType.MODULE.equals(dependencyVar.getDependencyType())) {
            uri = dependencyVar.getType().getMetaInfo().getSourceUrl();
        }
        return new FieldUsage(syntaxNode, description, uri);
    }

    private static String getColumnType(DTColumnsDefinition dtColumnsDefinition) {
        if (dtColumnsDefinition.isCondition()) {
            return "condition";
        }
        return dtColumnsDefinition.isAction() ? "action" : "return";
    }

    /**
     * Describes a field by the type it is accessed from, or by the component type when an array type has no meta
     * info.
     */
    private static FieldUsage describeByType(IOpenField boundField, IOpenClass accessType, ISyntaxNode syntaxNode) {
        var type = accessType;
        var metaInfo = type.getMetaInfo();
        while (metaInfo == null && type.isArray()) {
            type = type.getComponentClass();
            metaInfo = type.getMetaInfo();
        }
        var identifierSyntaxNode = getIdentifierSyntaxNode(syntaxNode);
        var description = MethodUtil.printType(boundField.getType()) + " " + boundField.getName();
        String uri = null;
        if (metaInfo != null) {
            uri = metaInfo.getSourceUrl();
            description = metaInfo.getDisplayName(IMetaInfo.REGULAR) + "\n" + description;
        } else if (type != NullOpenClass.the && !(type instanceof DecisionTableDataType)) {
            description = MethodUtil.printType(type) + "\n" + description;
        } else {
            uri = getFieldTypeSourceUrl(boundField);
        }
        return new FieldUsage(identifierSyntaxNode, description, uri);
    }

    /**
     * Returns the source of the given type, or the source of the field type when the given type has no meta info.
     */
    private static String getSourceUrl(IOpenClass type, IOpenField boundField) {
        var metaInfo = type.getMetaInfo();
        return metaInfo != null ? metaInfo.getSourceUrl() : getFieldTypeSourceUrl(boundField);
    }

    private static String getFieldTypeSourceUrl(IOpenField boundField) {
        var mi = boundField.getType().getMetaInfo();
        return mi != null ? mi.getSourceUrl() : null;
    }

    private static String concatenateSpreadsheetResultTables(Collection<CustomSpreadsheetResultOpenClass> types) {
        var sb = new StringBuilder();
        var stringLengthExceeded = false;
        var concatenated = 0;
        for (CustomSpreadsheetResultOpenClass c : types.size() > 3 ? new ArrayList<>(types).subList(0, 3) : types) {
            concatenated++;
            var sb1 = getTableNames(c);
            if (sb.length() + sb1.length() > MAX_DESCRIPTION_CLASS_LENGTH) {
                stringLengthExceeded = true;
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(sb1);
            concatenated++;
        }
        var more = types.size() - concatenated;
        if (types.size() > MAX_DESCRIPTION_CLASS_NUMBER || stringLengthExceeded) {
            sb.append("...(").append(more).append(") more");
        }
        return sb.toString();
    }

    /**
     * Lists the table names of a spreadsheet result type, or of each type a combined spreadsheet result type joins.
     */
    private static StringBuilder getTableNames(CustomSpreadsheetResultOpenClass c) {
        var sb1 = new StringBuilder();
        if (c instanceof CombinedSpreadsheetResultOpenClass class1) {
            for (CustomSpreadsheetResultOpenClass t : class1.getCombinedTypes()) {
                if (!sb1.isEmpty()) {
                    sb1.append(", ");
                }
                sb1.append(t.getName().substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()));
            }
        } else if (c != null) {
            if (!sb1.isEmpty()) {
                sb1.append(", ");
            }
            sb1.append(c.getName().substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()));
        } else {
            throw new IllegalStateException();
        }
        return sb1;
    }

    private static ISyntaxNode getIdentifierSyntaxNode(ISyntaxNode syntaxNode) {
        if ("function".equals(syntaxNode.getType())) {
            syntaxNode = syntaxNode.getChild(syntaxNode.getNumberOfChildren() - 1);
        }
        return syntaxNode;
    }

    /**
     * The node a field usage covers, its hover text and the source of the field declaration.
     */
    private record FieldUsage(ISyntaxNode syntaxNode, String description, String uri) {
    }

    private static class Holder {
        private static final FieldBoundNodeUsageCreator INSTANCE = new FieldBoundNodeUsageCreator();
    }

    public static FieldBoundNodeUsageCreator getInstance() {
        return Holder.INSTANCE;
    }

}
