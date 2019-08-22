package org.openl.binding.impl;

import java.util.*;
import java.util.stream.Collectors;

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
        Set<String> rowNames = new LinkedHashSet<>();
        rowNames.addAll(Arrays.stream(type1.getRowNames()).collect(Collectors.toCollection(HashSet::new)));
        rowNames.addAll(Arrays.stream(type2.getRowNames()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> columnNames = new LinkedHashSet<>();
        columnNames.addAll(Arrays.stream(type1.getColumnNames()).collect(Collectors.toCollection(HashSet::new)));
        columnNames.addAll(Arrays.stream(type2.getColumnNames()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> rowNamesMarkedWithAsterisk = new LinkedHashSet<>();
        rowNamesMarkedWithAsterisk
            .addAll(Arrays.stream(type1.getRowNamesMarkedWithAsterisk()).collect(Collectors.toCollection(HashSet::new)));
        rowNamesMarkedWithAsterisk
            .addAll(Arrays.stream(type2.getRowNamesMarkedWithAsterisk()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> columnNamesMarkedWithAsterisk = new LinkedHashSet<>();
        columnNamesMarkedWithAsterisk
            .addAll(Arrays.stream(type1.getColumnNamesMarkedWithAsterisk()).collect(Collectors.toCollection(HashSet::new)));
        columnNamesMarkedWithAsterisk
            .addAll(Arrays.stream(type2.getColumnNamesMarkedWithAsterisk()).collect(Collectors.toCollection(HashSet::new)));

        CustomSpreadsheetResultOpenClass mergedCustomSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            "IfNode" + type1.getName() + "And" + type2.getName(),
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}),
            rowNamesMarkedWithAsterisk.toArray(new String[] {}),
            columnNamesMarkedWithAsterisk.toArray(new String[] {}),
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}),
            type1.getModule());

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
