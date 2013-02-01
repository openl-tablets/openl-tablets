package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.IntType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.IntValue;

public class IntValueType extends IntType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new IntValue(reader.getValueAsInt());
    }
}