package org.openl.rules.calc;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.module.RootDictionaryContext;
import org.openl.binding.impl.module.VariableInContextFinder;
import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class SpreadsheetResultRootDictionaryContext extends RootDictionaryContext {
    public SpreadsheetResultRootDictionaryContext(IOpenField localVar, int maxDepthLevel) {
        super(new IOpenField[]{localVar}, maxDepthLevel);
    }

    @Override
    public IOpenField findField(String fieldName, boolean strictMatch) {
        String name = strictMatch ? fieldName : fieldName.toLowerCase();
        List<IOpenField> ff = strictMatch ? fields.get(name) : lowerCaseFields.get(name);

        if (ff == null) {
            var field = getRootField().getType().getField(fieldName, strictMatch);
            if (field != null) {
                initializeField(getRootField(), field, 1);
                ff = strictMatch ? fields.get(name) : lowerCaseFields.get(name);
            }
        }

        if (ff == null) {
            return null;
        }
        if (ff.size() > 1) {
            throw new AmbiguousFieldException(fieldName, ff);
        }

        return ff.getFirst();
    }

    /**
     * Finds a cell of the element of an array index operator.
     *
     * <p>An element of the {@code SpreadsheetResult} type can hold the result of any spreadsheet, so it has every
     * cell that a spreadsheet of the module has. When the cell is a step of the spreadsheet with the operator and of
     * no other spreadsheet, an element without the cell, a result of another spreadsheet, gives the value of the step.
     * An element with the cell gives its own value, as the results of the same spreadsheet do.
     *
     * <p>The step stands in only when it and the steps it reads come before the formula with the operator, the step
     * fits the type of the cell of the element, and the operator is not inside another array index operator;
     * otherwise an element without the cell gives an empty value. An element of the type of one spreadsheet, such as
     * {@code SpreadsheetResultPremium}, has only the cells of that spreadsheet.
     */
    @Override
    public @Nullable IOpenField findElementField(String fieldName,
                                                 boolean strictMatch,
                                                 VariableInContextFinder enclosing) {
        var field = findField(fieldName, strictMatch);
        if (field == null || !(getRootField().getType() instanceof SpreadsheetResultOpenClass type)) {
            return field;
        }
        // a cell more spreadsheets have can be the cell of any element, so only a cell of one spreadsheet stands in
        var cell = type.getField(fieldName, strictMatch);
        var declaringClasses = cell instanceof IOriginalDeclaredClassesOpenField declared
                ? declared.getDeclaringClasses()
                : IOpenClass.EMPTY;
        if (declaringClasses.length != 1) {
            return field;
        }
        var step = findStep(fieldName, strictMatch, enclosing);
        if (step == null) {
            return field;
        }
        var builder = step.getSpreadsheetStructureBuilder();
        if (builder == null) {
            return field;
        }
        var elementOrStep = new ElementOrStepField(getRootField(), field, step);
        return builder.standInForElements(elementOrStep, declaringClasses[0]) ? elementOrStep : field;
    }

    /**
     * Finds the step of the spreadsheet with the operator that has the name of the cell.
     */
    private static @Nullable SpreadsheetCellField findStep(String fieldName,
                                                         boolean strictMatch,
                                                         VariableInContextFinder enclosing) {
        try {
            return enclosing.findVariable(fieldName, strictMatch) instanceof SpreadsheetCellField step ? step : null;
        } catch (AmbiguousFieldException e) {
            // an ambiguous name around the operator leaves the cell of the element, as before
            return null;
        }
    }

    private IOpenField getRootField() {
        return roots[0];
    }

}
