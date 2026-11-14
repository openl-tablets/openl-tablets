package org.openl.rules.data;

import lombok.Getter;

import org.openl.rules.testmethod.result.Tolerance;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * A field of a test result that is compared within the precision of its column or of its test table.
 */
@Getter
public class PrecisionFieldChain extends FieldChain {

    /**
     * How far the returned value may be from the expected one.
     */
    private final Tolerance tolerance;

    /**
     * The number of the column of the test table that holds the expected value.
     */
    private final int column;

    public PrecisionFieldChain(IOpenClass type, IOpenField[] fields, Tolerance tolerance, int column) {
        super(type, fields);
        this.tolerance = tolerance;
        this.column = column;
    }
}
