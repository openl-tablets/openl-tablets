package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.ByteType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.ByteValue;

public class ByteValueType extends ByteType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        Byte value = (Byte) super.readObject(reader, context);
        return value == null ? null : new ByteValue(value);
    }
}
