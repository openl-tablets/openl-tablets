package org.openl.meta.explanation;

import java.util.Arrays;

import org.openl.meta.number.Formulas;

/**
 * Explanation implementation for formulas.
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 * @author DLiauchuk
 */
public class FormulaExplanationValue<T extends ExplanationNumberValue<T>> extends SingleValueExplanation<T> {

    private Formulas operand;

    private T v1;
    private T v2;

    public FormulaExplanationValue(T v1, T v2, Formulas operand) {
        this.v1 = v1;
        this.v2 = v2;
        this.operand = operand;
    }

    /**
     * @return the string representation of formula operand.
     */
    public String getOperand() {
        return operand.toString();
    }

    /**
     * @return the first formula argument.
     */
    public T getV1() {
        return v1;
    }

    /**
     * @return the second formula argument.
     */
    public T getV2() {
        return v2;
    }

    /**
     * @return true if formula is multiplicative.
     */
    public boolean isMultiplicative() {
        return operand.isMultiplicative();
    }

    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement<T>> getChildren() {
        return Arrays.asList(v1, v2);
    }

    @Override
    public String getType() {
        return String.format("formula.%s", operand);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
