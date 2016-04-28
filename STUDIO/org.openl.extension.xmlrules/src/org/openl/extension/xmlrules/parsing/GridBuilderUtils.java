package org.openl.extension.xmlrules.parsing;

import java.util.List;

import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.ProjectData;
import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.lazy.LazyAttributes;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.*;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.RangeNode;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.LessThanConstraint;
import org.openl.rules.table.constraints.MoreThanConstraint;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

public final class GridBuilderUtils {
    private GridBuilderUtils() {
    }

    public static ParseError createError(int gridRow, int gridColumn, Cell cell, Exception e) {
        RangeNode address = cell.getAddress();
        String errorMessage = String.format("Error in cell %s : %s", address.getAddress(), e.getMessage());
        return new ParseError(gridRow, gridColumn, errorMessage);
    }

    public static void addAttributes(StringGridBuilder gridBuilder, List<Attribute> attributes) {
        if (!attributes.isEmpty()) {
            int height = attributes.size();

            int row = gridBuilder.getRow();
            int column = gridBuilder.getColumn();

            try {
                gridBuilder.setCell(column, row, 1, height, "properties");
                gridBuilder.setStartColumn(column + 1);
                gridBuilder.setRow(row);

                for (Attribute attribute : attributes) {
                    TablePropertyDefinition matchedProperty = getTablePropertyDefinition(attribute);

                    String attributeName = matchedProperty != null ? matchedProperty.getName() : attribute.getName();

                    gridBuilder.addCell(attributeName);
                    gridBuilder.addCell(convertAttributeValue(attribute));
                    gridBuilder.nextRow();
                }
            } finally {
                gridBuilder.setRow(row + height);
                gridBuilder.setStartColumn(column);
            }

        }
    }

    private static String convertAttributeValue(Attribute attribute) {
        String value = attribute.getValue();

        LazyAttributes projectAttributes = ProjectData.getCurrentInstance().getAttributes();
        AttributeType type = AttributeType.STRING;
        for (MainAttribute mainAttribute : projectAttributes.getInstance().getItems().getAttribute()) {
            if (mainAttribute.getName().equals(attribute.getName())) {
                type = mainAttribute.getType();
                break;
            }
        }

        if (type == AttributeType.DATE) {
            return HelperFunctions.convertArgument(String.class, HelperFunctions.toDate(value));
        } else {
            return value;
        }
    }

    private static TablePropertyDefinition getTablePropertyDefinition(Attribute attribute) {
        List<TablePropertyDefinition> dimensionalTableProperties = TablePropertyDefinitionUtils.getDimensionalTableProperties();

        Class<? extends Constraint> constraintClass = getConstraintClass(attribute);
        for (TablePropertyDefinition property : dimensionalTableProperties) {
            String contextAttribute = property.getExpression().getMatchExpression().getContextAttribute();
            if (contextAttribute.equals(attribute.getName())) {
                Constraints constraints = property.getConstraints();
                if (constraints.size() == 1) {
                    Constraint constraint = constraints.get(0);
                    if (constraintClass == null || constraintClass.isAssignableFrom(constraint.getClass())) {
                        return property;
                    }
                }
            }
        }

        return null;
    }

    private static Class<? extends Constraint> getConstraintClass(Attribute attribute) {
        AttributeCondition condition = attribute.getCondition() == null ? AttributeCondition.Equals : AttributeCondition.valueOf(attribute.getCondition());
        // Constraint is opposite to condition. For example, for effectiveDate:
        // 1) le(currentDate) (same meaning as currentDate > effectiveDate) is condition GreaterThan
        // 2) effectiveDate < expirationDate is constraint LessThanConstraint
        // Both for the same property.
        // TODO refactor the code below to make it clear, for example, use condition from expression instead of constraint
        switch (condition) {
            case Equals:
                return null;
            case GreaterThan:
                return LessThanConstraint.class;
            case LessThan:
                return MoreThanConstraint.class;
        }

        throw new UnsupportedOperationException("Unsupported condition '" + condition + "'");
    }

    public static String wrapWithConvertFunctionIfNeeded(String returnType, String componentType, boolean isRange, String expressionToWrap) {
        if ("Object".equals(returnType) || isRange && "Object[][]".equals(returnType)) {
            // Cell() and CellRange() already return Object and Object[][] respectively
            return expressionToWrap;
        } else {
            String expression;
            if (returnType.endsWith("[]")) {
                if (isRange) {
                    expression = String.format("convertToRange(new %s[0][0], %s)", componentType, expressionToWrap);
                }
                else {
                    expression = String.format("convertToArray(new %s[0], %s)", componentType, expressionToWrap);
                }
            } else {
                expression = String.format("convert(new %s[0], %s)", componentType, expressionToWrap);
            }

            if (!"Object".equals(componentType)) {
                expression = String.format("(%s) %s", returnType, expression);
            }

            return expression;
        }
    }

    public static String getCellExpression(ExtensionModule module, String workbookName, String sheetName, CellReference cellReference) {
        return getCellExpression(workbookName, sheetName, cellReference, getCell(module, cellReference));
    }

    public static String getCellExpression(String workbookName, String sheetName, CellReference reference, Cell cell) {
        Node node = cell.getNode();
        if (node == null) {
            throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                    .getAddress()
                    .toOpenLString() + " contains incorrect value. It will be skipped");
        }

        ExpressionContext expressionContext = new ExpressionContext();
        expressionContext.setCurrentRow(reference.getRowNumber());
        expressionContext.setCurrentColumn(reference.getColumnNumber());
        expressionContext.setCanHandleArrayOperators(false);
        ExpressionContext.setInstance(expressionContext);

        node.setRootNode(true);
        return node.toOpenLString();
    }

    public static Cell getCell(ExtensionModule module, CellReference cellReference) {
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet sheet : workbook.getSheets()) {
                String workbookName = sheet.getWorkbookName();
                String sheetName = sheet.getName();

                for (LazyCells cells : sheet.getCells()) {
                    for (Cell c : cells.getCells()) {
                        if (CellReference.parse(workbookName, sheetName, c.getAddress()).equals(cellReference)) {
                            return c;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("Can't find the cell declaration: " + cellReference.getStringValue());
    }
}
