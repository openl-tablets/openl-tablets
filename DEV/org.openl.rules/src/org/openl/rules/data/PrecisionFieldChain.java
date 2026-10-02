package org.openl.rules.data;

import java.math.BigDecimal;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class PrecisionFieldChain extends FieldChain {
    private final Integer precision;

    public PrecisionFieldChain(IOpenClass type, IOpenField[] fields, Integer precision) {
        super(type, fields);
        this.precision = precision;
    }

    /**
     * Returns the difference that the precision allows: 10 raised to the power of the negated precision, exactly, so
     * precision 5 gives {@code 0.00001} and precision -2 gives {@code 100}.
     *
     * @return the delta, or {@code null} when the field has no precision
     */
    public BigDecimal getDelta() {
        if (precision != null) {
            return BigDecimal.ONE.scaleByPowerOfTen(-precision);
        }

        return null;
    }

    public boolean hasDelta() {
        return precision != null;
    }

}
