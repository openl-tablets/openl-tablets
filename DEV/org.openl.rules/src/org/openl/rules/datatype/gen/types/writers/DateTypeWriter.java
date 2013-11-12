package org.openl.rules.datatype.gen.types.writers;

import org.openl.rules.datatype.gen.FieldDescription;

public class DateTypeWriter extends ObjectTypeWriter {
    @Override
    protected String updateValue(FieldDescription fieldType) {
        return fieldType.getDefaultValueAsString();
    }

}
