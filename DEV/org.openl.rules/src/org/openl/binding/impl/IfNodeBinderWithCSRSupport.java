package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

public class IfNodeBinderWithCSRSupport extends IfNodeBinder {

    private static CustomSpreadsheetResultOpenClass mergeTwoCustomSpreadsheetResultTypes(
            CustomSpreadsheetResultOpenClass type1,
            CustomSpreadsheetResultOpenClass type2) {
        if (!type1.getModule().equals(type2.getModule())) {
            throw new IllegalStateException(
                "Unexpected state: custom spreadsheet result types are from different modules.");
        }
        return type1.getModule().buildOrGetUnifiedSpreadsheetResult(type1, type2);
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
            if (!type1.equals(type2) && type1.getModule() == type2.getModule()) {
                return new IfNode(node,
                    conditionNode,
                    thenNode,
                    elseNode,
                    mergeTwoCustomSpreadsheetResultTypes(type1, type2));
            }
        }
        return super.buildIfElseNode(node, bindingContext, conditionNode, thenNode, type, elseNode, elseType);
    }

}
