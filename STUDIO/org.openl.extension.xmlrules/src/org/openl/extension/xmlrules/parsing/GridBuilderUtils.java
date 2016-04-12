package org.openl.extension.xmlrules.parsing;

import java.util.List;

import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.AttributeCondition;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.node.RangeNode;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
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

                for (Attribute attribute : attributes) {
                    TablePropertyDefinition matchedProperty = getTablePropertyDefinition(attribute);

                    String attributeName = matchedProperty != null ? matchedProperty.getName() : attribute.getName();

                    gridBuilder.addCell(attributeName);
                    gridBuilder.addCell(attribute.getValue());
                    gridBuilder.nextRow();
                }
            } finally {
                gridBuilder.setRow(row + height);
                gridBuilder.setStartColumn(column);
            }

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
        switch (condition) {
            case Equals:
                return null;
            case MoreThan:
                return MoreThanConstraint.class;
            case LessThan:
                return LessThanConstraint.class;
        }

        throw new UnsupportedOperationException("Unsupported condition '" + condition + "'");
    }
}
