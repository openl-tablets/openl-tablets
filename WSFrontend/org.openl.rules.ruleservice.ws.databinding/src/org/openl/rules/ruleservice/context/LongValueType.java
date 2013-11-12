package org.openl.rules.ruleservice.context;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


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