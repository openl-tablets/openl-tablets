package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

import java.math.BigInteger;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.BigIntegerType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.BigIntegerValue;

public class BigIntegerValueType extends BigIntegerType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        BigInteger value = (BigInteger) super.readObject(reader, context);
        return value == null ? null : new BigIntegerValue(value);
    }
}
