package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.FloatType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.FloatValue;

public class FloatValueType extends FloatType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new FloatValue(reader.getValueAsFloat());
    }
}