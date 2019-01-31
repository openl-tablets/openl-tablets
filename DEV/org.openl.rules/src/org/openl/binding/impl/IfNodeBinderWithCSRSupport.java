package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.CustomSpreadsheetResultField;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class IfNodeBinderWithCSRSupport extends IfNodeBinder {

    private static CustomSpreadsheetResultOpenClass merge(CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2) {
        List<String> rowNames = new ArrayList<>();
        for (String rowName : type1.getRowNames()) {
            rowNames.add(rowName);
        }
        for (String rowName : type2.getRowNames()) {
            if (!rowNames.contains(rowName)) {
                rowNames.add(rowName);
            }
        }

        List<String> columnNames = new ArrayList<>();
        for (String colName : type1.getColumnNames()) {
            columnNames.add(colName);
        }
        for (String colName : type2.getColumnNames()) {
            if (!columnNames.contains(colName)) {
                columnNames.add(colName);
            }
        }

        CustomSpreadsheetResultOpenClass mergedCustomSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            "SpreadsheetResult",
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}),
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}));
        Map<String, IOpenField> fields1 = type1.getFields();
        Map<String, IOpenField> fields2 = type2.getFields();
        Set<String> fieldNames = new HashSet<>();
        fieldNames.addAll(fields1.keySet());
        fieldNames.addAll(fields2.keySet());
        for (String fieldName : fieldNames) {
            boolean f1 = fields1.containsKey(fieldName);
            boolean f2 = fields2.containsKey(fieldName);
            if (f1 && f2) {
                IOpenField field1 = fields1.get(fieldName);
                IOpenField field2 = fields2.get(fieldName);
                if (field1.getType().equals(field2.getType())) {
                    mergedCustomSpreadsheetResultOpenClass
                        .addField(new CustomSpreadsheetResultField(mergedCustomSpreadsheetResultOpenClass,
                            fieldName,
                            field1.getType()));
                } else {
                    mergedCustomSpreadsheetResultOpenClass
                        .addField(new CustomSpreadsheetResultField(mergedCustomSpreadsheetResultOpenClass,
                            fieldName,
                            JavaOpenClass.OBJECT));
                }
            } else {
                if (f1) {
                    mergedCustomSpreadsheetResultOpenClass.addField(fields1.get(fieldName));
                } else {
                    mergedCustomSpreadsheetResultOpenClass.addField(fields2.get(fieldName));
                }
            }
        }
        return mergedCustomSpreadsheetResultOpenClass;
    }

    @Override
    protected IBoundNode buildIfElseNode(ISyntaxNode node,
            IBindingContext bindingContext,
            IBoundNode conditionNode,
            IBoundNode thenNode,
            IOpenClass type,
            IBoundNode elseNode,
            IOpenClass elseType) {
        if (type instanceof CustomSpreadsheetResultOpenClass && elseType instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass type1 = (CustomSpreadsheetResultOpenClass) type;
            CustomSpreadsheetResultOpenClass type2 = (CustomSpreadsheetResultOpenClass) elseType;
            return new IfNode(node, conditionNode, thenNode, elseNode, merge(type1, type2));
        } else {
            return super.buildIfElseNode(node, bindingContext, conditionNode, thenNode, type, elseNode, elseType);
        }
    }

}
