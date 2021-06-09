package org.openl.meta.explanation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.openl.meta.number.CastOperand;

@SuppressWarnings("rawtypes")
public class CastExplanationValue extends SingleValueExplanation {
    private final ExplanationNumberValue<?> value;
    private final CastOperand operand;

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
    public Collection<ExplanationNumberValue<?>> getChildren() {
        return Collections.singletonList(value);
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
