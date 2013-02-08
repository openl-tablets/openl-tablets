package org.openl.rules.ruleservice.context;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.BigIntegerType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.BigIntegerValue;

public class BigIntegerValueType extends BigIntegerType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new BigIntegerValue(reader.getValue().trim());
    }
}