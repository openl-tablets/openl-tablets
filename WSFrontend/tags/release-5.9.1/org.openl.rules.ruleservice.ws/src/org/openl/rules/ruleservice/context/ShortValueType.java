package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.ShortType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.ShortValue;

public class ShortValueType extends ShortType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new ShortValue(reader.getValue().trim());
    }
}