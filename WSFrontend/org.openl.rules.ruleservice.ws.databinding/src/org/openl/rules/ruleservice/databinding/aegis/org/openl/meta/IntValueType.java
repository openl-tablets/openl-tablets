package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.IntType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.IntValue;

public class IntValueType extends IntType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        Integer value = (Integer) super.readObject(reader, context);
        return value == null ? null : new IntValue(value);
    }
}