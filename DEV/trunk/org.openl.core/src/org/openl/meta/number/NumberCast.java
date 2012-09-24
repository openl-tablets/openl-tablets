package org.openl.meta.number;

import org.openl.meta.explanation.ExplanationNumberValue;

public class NumberCast {
    private ExplanationNumberValue<?> value;
    private CastOperand operand;

    public NumberCast(ExplanationNumberValue<?> value, CastOperand operand) {
        this.value = value;
        this.operand = operand;
    }

    public ExplanationNumberValue<?> getValue() {
        return value;
    }
    
    public CastOperand getOperand() {
        return operand;
    }
}
