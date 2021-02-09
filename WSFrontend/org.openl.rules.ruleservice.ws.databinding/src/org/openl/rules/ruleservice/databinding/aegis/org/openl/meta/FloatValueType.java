package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.FloatType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.FloatValue;

public class FloatValueType extends FloatType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        Float value = (Float) super.readObject(reader, context);
        return value == null ? null : new FloatValue(value);
    }
}