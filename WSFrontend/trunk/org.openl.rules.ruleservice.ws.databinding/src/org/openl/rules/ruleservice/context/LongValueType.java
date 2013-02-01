package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.LongType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.LongValue;

public class LongValueType extends LongType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new LongValue(reader.getValueAsLong());
    }
}