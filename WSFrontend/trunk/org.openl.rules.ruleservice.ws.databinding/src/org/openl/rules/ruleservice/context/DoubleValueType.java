package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.DoubleType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.DoubleValue;

public class DoubleValueType extends DoubleType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new DoubleValue(reader.getValueAsDouble());
    }
}