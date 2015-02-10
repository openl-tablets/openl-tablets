package org.openl.meta.explanation;

import java.util.Arrays;

import org.openl.meta.number.CastOperand;

@SuppressWarnings("rawtypes")
public class CastExplanationValue extends SingleValueExplanation {
    private ExplanationNumberValue<?> value;
    private CastOperand operand;

    public CastExplanationValue(ExplanationNumberValue<?> value, CastOperand operand) {
        this.value = value;
        this.operand = operand;
    }

    public ExplanationNumberValue<?> getValue() {
        return value;
    }

    public CastOperand getOperand() {
        return operand;
    }

    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement> getChildren() {
        return Arrays.asList(value);
    }

    @Override
    public String getType() {
        return String.format("cast.%s", operand.getType());
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
