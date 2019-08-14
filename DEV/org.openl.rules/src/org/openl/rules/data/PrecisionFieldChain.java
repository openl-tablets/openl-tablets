package org.openl.rules.data;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class PrecisionFieldChain extends FieldChain {
    private Integer precision;

    public PrecisionFieldChain(IOpenClass type, IOpenField[] fields, Integer precision) {
        super(type, fields);
        this.precision = precision;
    }

    public Double getDelta() {
        if (precision != null) {
            return Math.pow(10.0, -precision);
        }

        return null;
    }

    public boolean hasDelta() {
        return precision != null;
    }

}
