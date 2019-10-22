package org.openl.rules.ruleservice.databinding.aegis.org.openl.meta;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

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
