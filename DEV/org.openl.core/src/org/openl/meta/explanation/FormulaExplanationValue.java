package org.openl.meta.explanation;

import org.openl.meta.number.NumberFormula;

/**
 * Explanation implementation for formulas.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public class FormulaExplanationValue<T extends ExplanationNumberValue<T>> extends SingleValueExplanation<T> {

    private NumberFormula<T> formulaHolder;

    public FormulaExplanationValue(NumberFormula<T> formulaHolder) {
        this.formulaHolder = formulaHolder;
    }

    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement<T>> getChildren() {
        return formulaHolder.getArguments();
    }

    @Override
    public String getType() {
        return String.format("formula.%s", formulaHolder.getOperand());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
