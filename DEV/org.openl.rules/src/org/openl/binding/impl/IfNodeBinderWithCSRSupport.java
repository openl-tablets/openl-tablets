package org.openl.binding.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.CustomSpreadsheetResultField;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;

public class IfNodeBinderWithCSRSupport extends IfNodeBinder {

    private static String getIfCSRTypeName(CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2) {
        return "IfNode" + type1.getName() + "And" + type2.getName() + "_" + type1.getName()
            .hashCode() + "_" + type2.getName().hashCode();
    }

    private static CustomSpreadsheetResultOpenClass merge(IBindingContext bindingContext,
            CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2) {

        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, getIfCSRTypeName(type1, type2));
        if (type instanceof CustomSpreadsheetResultOpenClass) {
            return (CustomSpreadsheetResultOpenClass) type;
        }

        Set<String> rowNames = new LinkedHashSet<>();
        rowNames.addAll(Arrays.stream(type1.getRowNames()).collect(Collectors.toCollection(HashSet::new)));
        rowNames.addAll(Arrays.stream(type2.getRowNames()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> columnNames = new LinkedHashSet<>();
        columnNames.addAll(Arrays.stream(type1.getColumnNames()).collect(Collectors.toCollection(HashSet::new)));
        columnNames.addAll(Arrays.stream(type2.getColumnNames()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> rowNamesForResultModel = new LinkedHashSet<>();
        rowNamesForResultModel.addAll(
            Arrays.stream(type1.getRowNamesForResultModel()).collect(Collectors.toCollection(HashSet::new)));
        rowNamesForResultModel.addAll(
            Arrays.stream(type2.getRowNamesForResultModel()).collect(Collectors.toCollection(HashSet::new)));

        Set<String> columnNamesForResultModel = new LinkedHashSet<>();
        columnNamesForResultModel.addAll(
            Arrays.stream(type1.getColumnNamesForResultModel()).collect(Collectors.toCollection(HashSet::new)));
        columnNamesForResultModel.addAll(
            Arrays.stream(type2.getColumnNamesForResultModel()).collect(Collectors.toCollection(HashSet::new)));

        if (!type1.getModule().equals(type2.getModule())) {
            throw new IllegalStateException("CSR types are from differnet modules.");
        }

        CustomSpreadsheetResultOpenClass mergedCustomSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
            getIfCSRTypeName(type1, type2),
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

        bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, mergedCustomSpreadsheetResultOpenClass);

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
            return new IfNode(node, conditionNode, thenNode, elseNode, merge(bindingContext, type1, type2));
        } else {
            return super.buildIfElseNode(node, bindingContext, conditionNode, thenNode, type, elseNode, elseType);
        }
    }

}
