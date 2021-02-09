package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.ShortType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.ShortValue;

public class ShortValueType extends ShortType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        Short value = (Short) super.readObject(reader, context);
        return value == null ? null : new ShortValue(value);
    }
}