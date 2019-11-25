package org.openl.binding.impl;

import java.util.*;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.CustomSpreadsheetResultField;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class IfNodeBinderWithCSRSupport extends IfNodeBinder {

    private static Set<String> toMergedLinkedHashSet(String[] arr1, String[] arr2) {
        Set<String> ret = new LinkedHashSet<>();
        Arrays.stream(arr1).forEach(ret::add);
        Arrays.stream(arr2).forEach(ret::add);
        return ret;
    }

    private static CustomSpreadsheetResultOpenClass mergeTwoCustomSpreadsheetResultTypes(
            CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2) {

        Set<String> rowNames = toMergedLinkedHashSet(type1.getRowNames(), type2.getRowNames());
        Set<String> columnNames = toMergedLinkedHashSet(type1.getColumnNames(), type2.getColumnNames());

        Set<String> rowNamesForResultModel = toMergedLinkedHashSet(type1.getRowNamesForResultModel(),
            type2.getRowNamesForResultModel());
        Set<String> columnNamesForResultModel = toMergedLinkedHashSet(type1.getColumnNamesForResultModel(),
            type2.getColumnNamesForResultModel());

        if (!type1.getModule().equals(type2.getModule())) {
            throw new IllegalStateException("Custom spreadsheet result types are from differnet modules.");
        }

        CustomSpreadsheetResultOpenClass mergedCustomSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            "IfNode_" + type1.getName() + "_And_" + type2.getName(),
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}),
            rowNamesForResultModel.toArray(new String[] {}),
            columnNamesForResultModel.toArray(new String[] {}),
            rowNames.toArray(new String[] {}),
            columnNames.toArray(new String[] {}),
            type1.getModule(),
            false);

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
                if (Objects.equals(field1.getType(), field2.getType())) {
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
            return new IfNode(node,
                conditionNode,
                thenNode,
                elseNode,
                mergeTwoCustomSpreadsheetResultTypes(type1, type2));
        } else {
            return super.buildIfElseNode(node, bindingContext, conditionNode, thenNode, type, elseNode, elseType);
        }
    }

}
