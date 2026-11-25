package org.openl.rules.calc;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.rules.calc.element.SpreadsheetCellField;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

/**
 * A cell of an element of the {@code SpreadsheetResult} type that gives the value of a step of the spreadsheet with
 * the operator for an element without the cell, once the spreadsheet lets the step stand in for the cell.
 */
final class ElementOrStepField extends OpenFieldDelegator {
    private final IOpenField element;
    @Getter(AccessLevel.PACKAGE)
    private final SpreadsheetCellField step;
    private boolean standsIn;

    /**
     * @param element the element of the operator
     * @param field   the field that reads the cell from the element
     * @param step    the step of the spreadsheet with the operator
     */
    ElementOrStepField(IOpenField element, IOpenField field, SpreadsheetCellField step) {
        super(field);
        this.element = element;
        this.step = step;
    }

    /**
     * Lets an element without the cell give the value of the step.
     */
    void standIn() {
        standsIn = true;
    }

    @Override
    public @Nullable Object get(@Nullable Object target, IRuntimeEnv env) {
        if (standsIn && element.get(target, env) instanceof SpreadsheetResult result && !result.hasField(getName())) {
            return step.get(target, env);
        }
        return super.get(target, env);
    }
}
