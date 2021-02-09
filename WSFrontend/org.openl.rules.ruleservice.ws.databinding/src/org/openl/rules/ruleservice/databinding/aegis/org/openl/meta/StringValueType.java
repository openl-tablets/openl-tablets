package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.StringValue;

public class StringValueType extends StringType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        String value = (String) super.readObject(reader, context);
        return value == null ? null : new StringValue(value);
    }
}