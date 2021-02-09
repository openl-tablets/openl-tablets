package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.LongType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.LongValue;

public class LongValueType extends LongType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        Long value = (Long) super.readObject(reader, context);
        return value == null ? null : new LongValue(value);
    }
}