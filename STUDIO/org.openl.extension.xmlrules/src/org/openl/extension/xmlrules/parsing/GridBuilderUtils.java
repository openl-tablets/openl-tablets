package org.openl.extension.xmlrules.parsing;

import java.util.List;

import org.openl.extension.xmlrules.ParseError;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.Cell;
import org.openl.extension.xmlrules.model.single.node.RangeNode;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;

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

            gridBuilder.setCell(column, row, 1, height, "properties");
            gridBuilder.setStartColumn(column + 1);

            for (Attribute attribute : attributes) {
                gridBuilder.addCell(attribute.getName());
                gridBuilder.addCell(attribute.getValue());
                gridBuilder.nextRow();
            }

            gridBuilder.setRow(row + height);
            gridBuilder.setStartColumn(column);
        }
    }
}
