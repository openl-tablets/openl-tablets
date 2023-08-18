package org.openl.rules.project.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.MethodUsage;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class OpenL2TextUtils {

    private OpenL2TextUtils() {
    }

    public static String methodToString(ExecutableRulesMethod rulesMethod,
            boolean replaceAliasesWithBaseTypes,
            boolean tableAsCode,
            boolean onlyMethodCells,
            int maxRows) {
        if (rulesMethod instanceof Spreadsheet) {
            return spreadsheetToString((Spreadsheet) rulesMethod,
                replaceAliasesWithBaseTypes,
                tableAsCode,
                onlyMethodCells);
        } else if (rulesMethod instanceof DecisionTable) {
            DecisionTable decisionTable = (DecisionTable) rulesMethod;
            return tableSyntaxNodeToString(decisionTable.getSyntaxNode(),
                decisionTable.getDtInfo() != null && decisionTable.getDtInfo().isTransposed(),
                replaceAliasesWithBaseTypes,
                maxRows);
        } else {
            return tableSyntaxNodeToString(rulesMethod.getSyntaxNode(),
                false,
                replaceAliasesWithBaseTypes,
                Integer.MAX_VALUE);
        }
    }

    private static String cellToString(ICell cell, MetaInfoReader metaInfoReader, boolean replaceAliasesWithBaseTypes) {
        String cellValue = cell.getStringValue();
        if (metaInfoReader != null && replaceAliasesWithBaseTypes) {
            CellMetaInfo cellMetaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
            if (cellMetaInfo != null && cellMetaInfo.getUsedNodes() != null) {
                List<NodeUsage> usedNodes = new ArrayList<>(cellMetaInfo.getUsedNodes());
                usedNodes.sort(Comparator.comparing(NodeUsage::getStart).reversed());
                for (NodeUsage nodeUsage : usedNodes) {
                    if (nodeUsage instanceof SimpleNodeUsage) {
                        SimpleNodeUsage simpleNodeUsage = (SimpleNodeUsage) nodeUsage;
                        if (simpleNodeUsage.getType() instanceof DomainOpenClass) {
                            cellValue = cellValue.substring(0, simpleNodeUsage.getStart()) + openClassToName(
                                ((DomainOpenClass) simpleNodeUsage.getType()).getBaseClass()) + cellValue
                                    .substring(simpleNodeUsage.getEnd());
                        }
                    }
                }
            }
        }
        // Replace special characters in cell value to avoid problems with parsing
        cellValue = resolveProblemsWithParsing(cellValue);
        return cellValue;
    }

    private static String formulaToExp(String formula) {
        formula = formula.trim();
        if (formula.startsWith("=")) {
            formula = formula.substring(1);
        }
        if (formula.trim().isEmpty()) {
            formula = "null";
        }
        return formula.trim();
    }

    private static Set<String> findFieldNames(Map<String, Point> fieldsCoordinates, int row, int column) {
        Set<String> fieldNames = new HashSet<>();
        for (Map.Entry<String, Point> e : fieldsCoordinates.entrySet()) {
            Point p = e.getValue();
            if (p.getRow() == row && p.getColumn() == column) {
                fieldNames.add(e.getKey());
            }
        }
        return fieldNames;
    }

    public static String spreadsheetToString(Spreadsheet spreadsheet,
            boolean replaceAliasesWithBaseTypes,
            boolean tableAsCode,
            boolean onlyMethodCells) {
        Function<ICell, String> cellToStr = (ICell cell) -> cellToString(cell,
            spreadsheet.getSyntaxNode().getMetaInfoReader(),
            replaceAliasesWithBaseTypes);
        StringBuilder sb = new StringBuilder();
        // Write table header
        if (!tableAsCode) {
            sb.append("|");
        }
        sb.append(cellToStr.apply(spreadsheet.getSyntaxNode().getTable().getSource().getCell(0, 0)));
        if (!tableAsCode) {
            sb.append("|".repeat(spreadsheet.getColumnNames().length)).append("\n");
            // Write column names
            sb.append("|").append("Steps");
            for (int i = 0; i < spreadsheet.getColumnNames().length; i++) {
                sb.append("|").append(spreadsheet.getColumnNames()[i]).append("|");
            }
        }
        sb.append("\n");
        if (tableAsCode) {
            for (int j = 0; j < spreadsheet.getRowNames().length; j++) {
                for (int i = 0; i < spreadsheet.getColumnNames().length; i++) {
                    SpreadsheetCell spreadsheetCell = spreadsheet.getCells()[j][i];
                    if ((!spreadsheetCell.isMethodCell() || NullOpenClass
                        .isAnyNull(spreadsheetCell.getType())) && onlyMethodCells) {
                        continue;
                    }
                    if (spreadsheetCell.isMethodCell()) {
                        sb.append(openClassToName(spreadsheetCell.getType()));
                    } else {
                        sb.append("var");
                    }
                    Set<String> fieldNames = spreadsheet.getColumnNames().length > 1 ? findFieldNames(spreadsheet
                        .getFieldsCoordinates(), j, i) : Collections.singleton("$" + spreadsheet.getRowNames()[j]);
                    // Join field names with comma
                    if (!fieldNames.isEmpty()) {
                        sb.append(" ").append(String.join(", ", fieldNames));
                    }
                    sb.append(" = ");
                    boolean isString = spreadsheetCell.isValueCell() && JavaOpenClass.STRING
                        .equals(spreadsheetCell.getType());
                    if (isString) {
                        sb.append("\"");
                    }
                    sb.append(formulaToExp(cellToStr.apply(spreadsheetCell.getSourceCell())));
                    if (isString) {
                        sb.append("\"");
                    }
                    // if sb doesn't end with ';' add it
                    if (sb.charAt(sb.length() - 1) != ';') {
                        sb.append(";");
                    }
                    sb.append("\n");
                }
            }
        } else {
            // Write row names and cell values
            for (int j = 0; j < spreadsheet.getRowNames().length; j++) {
                sb.append("|").append(spreadsheet.getRowNames()[j]).append("|");
                for (int i = 0; i < spreadsheet.getColumnNames().length; i++) {
                    String cellValue = cellToStr.apply(spreadsheet.getCells()[j][i].getSourceCell());
                    sb.append(cellValue).append("|");
                }
                // if not last row add new line
                if (j < spreadsheet.getRowNames().length - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public static String tableSyntaxNodeToString(TableSyntaxNode tableSyntaxNode,
            boolean transposeBody,
            boolean replaceAliasesWithBaseTypes,
            int maxRows) {
        Function<ICell, String> cellToStr = (
                ICell cell) -> cellToString(cell, tableSyntaxNode.getMetaInfoReader(), replaceAliasesWithBaseTypes);
        StringBuilder sb = new StringBuilder();
        IGridTable table = tableSyntaxNode.getTable().getSource();
        int height = table.getHeight();
        int width = table.getWidth();
        sb.append("|").append(cellToStr.apply(table.getCell(0, 0)));
        int d = table.getCell(0, 0).getHeight();
        // Empty dt body
        if (d >= height) {
            sb.append("|".repeat(width)).append("\n");
            return sb.toString();
        }
        // Skip the properties section
        if (table.getCell(0, d).getStringValue() != null && "properties"
            .equalsIgnoreCase(table.getCell(0, d).getStringValue().trim())) {
            d += table.getCell(0, d).getHeight();
        }
        table = table.getSubtable(0, d, width, height - d);
        // Empty dt body returns null
        if (table == null) {
            sb.append("|".repeat(width)).append("\n");
            return sb.toString();
        }
        height = table.getHeight();
        // Transpose the table if it is required
        if (transposeBody) {
            table = table.transpose();
            height = table.getHeight();
            width = table.getWidth();
        }
        sb.append("|".repeat(width)).append("\n");
        for (int i = 0; i < height; i++) {
            if (i >= maxRows) {
                sb.append("...").append("\n");
                break;
            }
            sb.append("|");
            for (int j = 0; j < width; j++) {
                String cellValue = cellToStr.apply(table.getCell(j, i));
                sb.append(cellValue);
                sb.append("|");
            }
            // if not last row add new line
            if (i < height - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static String openClassToName(IOpenClass openClass) {
        return openClass.getDisplayName(INamedThing.SHORT);
    }

    public static String openClassToString(IOpenClass openClass, boolean replaceAliasesWithBaseTypes) {
        StringBuilder sb = new StringBuilder();
        if (openClass instanceof DomainOpenClass) {
            if (replaceAliasesWithBaseTypes) {
                return openClassToName(((DomainOpenClass) openClass).getBaseClass());
            }
            DomainOpenClass domainOpenClass = (DomainOpenClass) openClass;
            sb.append("vocabulary ")
                .append(openClassToName(domainOpenClass))
                .append(" : ")
                .append(openClassToName(domainOpenClass.getBaseClass()))
                .append(" {")
                .append("\n");
            Iterator<?> itr = domainOpenClass.getDomain().iterator();
            while (itr.hasNext()) {
                sb.append("\t");
                if (JavaOpenClass.STRING.equals(domainOpenClass.getBaseClass())) {
                    sb.append("\"");
                }
                sb.append(itr.next());
                if (JavaOpenClass.STRING.equals(domainOpenClass.getBaseClass())) {
                    sb.append("\"");
                }
                if (itr.hasNext()) {
                    sb.append(", ");
                }
                sb.append("\n");
            }
            sb.append("}");
        } else {
            sb.append("class ").append(openClassToName(openClass)).append(" {").append("\n");
            for (IOpenField openField : openClass.getFields()) {
                IOpenClass type = openField.getType();
                if (type instanceof DomainOpenClass) {
                    type = ((DomainOpenClass) type).getBaseClass();
                }
                sb.append("\t").append(openClassToName(type)).append(" ").append(openField.getName()).append(";\n");
            }
            sb.append("}");
        }
        return sb.toString();
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    public static String dimensionalPropertiesToString(ExecutableRulesMethod rulesMethod) {
        return dimensionalPropertiesToString(rulesMethod, createObjectMapper());
    }

    public static String dimensionalPropertiesToString(ExecutableRulesMethod rulesMethod, ObjectMapper objectMapper) {
        if (rulesMethod.getMethodProperties() != null && !rulesMethod.getMethodProperties()
            .getAllDimensionalProperties()
            .isEmpty()) {
            if (objectMapper == null) {
                objectMapper = createObjectMapper();
            }
            try {
                Map<String, Object> props = new HashMap<>(
                    rulesMethod.getMethodProperties().getAllDimensionalProperties());
                if (props.containsKey("state")) {
                    UsStatesEnum[] states = (UsStatesEnum[]) props.get("state");
                    props.put("state", Arrays.stream(states).map(UsStatesEnum::toString).toArray());
                }
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(props);
            } catch (JsonProcessingException e) {
                return null;
            }
        }
        return null;
    }

    public static Set<IOpenMethod> methodRefs(TableSyntaxNode tableSyntaxNode) {
        Set<IOpenMethod> methodRefs = new HashSet<>();
        MetaInfoReader metaInfoReader = tableSyntaxNode.getMetaInfoReader();
        if (metaInfoReader != null) {
            int height = tableSyntaxNode.getTable().getSource().getHeight();
            int width = tableSyntaxNode.getTable().getSource().getWidth();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    ICell cell = tableSyntaxNode.getTable().getSource().getCell(j, i);
                    CellMetaInfo cellMetaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(),
                        cell.getAbsoluteColumn());
                    if (cellMetaInfo != null && cellMetaInfo.getUsedNodes() != null) {
                        for (NodeUsage nodeUsage : cellMetaInfo.getUsedNodes()) {
                            if (nodeUsage instanceof MethodUsage) {
                                methodRefs.add(((MethodUsage) nodeUsage).getMethod());
                            }
                        }
                    }
                }
            }
        }
        return methodRefs;
    }

    public static Set<IOpenClass> methodTypes(TableSyntaxNode tableSyntaxNode) {
        Set<IOpenClass> types = new HashSet<>();
        MetaInfoReader metaInfoReader = tableSyntaxNode.getMetaInfoReader();
        if (metaInfoReader != null) {
            int height = tableSyntaxNode.getTable().getSource().getHeight();
            int width = tableSyntaxNode.getTable().getSource().getWidth();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    ICell cell = tableSyntaxNode.getTable().getSource().getCell(j, i);
                    CellMetaInfo cellMetaInfo = metaInfoReader.getMetaInfo(cell.getAbsoluteRow(),
                        cell.getAbsoluteColumn());
                    if (cellMetaInfo != null && cellMetaInfo.getUsedNodes() != null) {
                        for (NodeUsage nodeUsage : cellMetaInfo.getUsedNodes()) {
                            if (nodeUsage instanceof SimpleNodeUsage) {
                                SimpleNodeUsage simpleNodeUsage = (SimpleNodeUsage) nodeUsage;
                                if (simpleNodeUsage.getType() != null) {
                                    types.add(simpleNodeUsage.getType());
                                }
                            }
                        }
                    }
                }
            }
        }
        return types;
    }

    private static String resolveProblemsWithParsing(String cellValue) {
        if (cellValue == null) {
            return "";
        }
        cellValue = cellValue.trim();
        cellValue = cellValue.replaceAll("\n", ";");
        if (cellValue.startsWith("=")) {
            return cellValue.replaceAll("\\|\\|", "or");
        } else if (cellValue.contains("|")) {
            return "\"" + cellValue.replaceAll("\"", "\\\\\"");
        }
        return cellValue;
    }

    public static void collectTypes(IOpenClass type,
            Set<IOpenClass> container,
            int maxDepth,
            boolean replaceAliasesWithBaseTypes) {
        collectTypes(type, container, 0, maxDepth, replaceAliasesWithBaseTypes);
    }

    private static void collectTypes(IOpenClass type,
            Set<IOpenClass> container,
            int depth,
            int maxDepth,
            boolean replaceAliasesWithBaseTypes) {
        if (depth > maxDepth) {
            return;
        }
        while (type.isArray()) {
            type = type.getComponentClass();
        }
        if (container.contains(type)) {
            return;
        }
        if (NullOpenClass.isAnyNull(type)) {
            return;
        }
        try {
            // Try to load the class to check if it is a valid type
            type.getFields();
        } catch (LinkageError ignored) {
            return;
        }
        if (!(type instanceof DomainOpenClass)) {
            // Skip primitive types and types from java.lang, java.util and java.math packages
            if (type.getInstanceClass() != null && type.getInstanceClass().isPrimitive()) {
                return;
            }
            String className = type.getInstanceClass() != null ? type.getInstanceClass().getName() : type.getName();
            if (className.startsWith("java.lang.") || className.startsWith("java.util.") || className
                .startsWith("java.math.")) {
                return;
            }
            if (type.getFields().isEmpty()) {
                return;
            }
        }
        if (type instanceof DomainOpenClass && replaceAliasesWithBaseTypes) {
            return;
        }
        container.add(type);
        // Recursively expands the openClass and adds any encountered types to the openClasses set
        for (IOpenField openField : type.getFields()) {
            collectTypes(openField.getType(), container, depth + 1, maxDepth, replaceAliasesWithBaseTypes);
        }
    }

    private static String typeToString(IOpenClass openClass, boolean replaceAliasesWithBaseTypes) {
        IOpenClass t = openClass;
        if (replaceAliasesWithBaseTypes && t instanceof DomainOpenClass) {
            t = ((DomainOpenClass) t).getBaseClass();
        }
        return OpenL2TextUtils.openClassToName(t);
    }

    public static String methodHeaderToString(IOpenMethod method, boolean replaceAliasesWithBaseTypes) {
        StringBuilder sb1 = new StringBuilder();
        sb1.append(typeToString(method.getType(), replaceAliasesWithBaseTypes)).append(" ");
        MethodUtil.printMethod(method, sb1, e -> typeToString(e, replaceAliasesWithBaseTypes));
        return sb1.toString();
    }
}
