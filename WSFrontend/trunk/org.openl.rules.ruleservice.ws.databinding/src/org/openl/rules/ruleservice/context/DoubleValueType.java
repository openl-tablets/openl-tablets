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
import org.apache.cxf.aegis.type.basic.DoubleType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.DoubleValue;

public class DoubleValueType extends DoubleType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new DoubleValue(reader.getValueAsDouble());
    }
}