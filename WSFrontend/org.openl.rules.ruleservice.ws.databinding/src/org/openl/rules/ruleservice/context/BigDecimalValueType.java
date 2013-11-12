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
import org.apache.cxf.aegis.type.basic.BigDecimalType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.meta.BigDecimalValue;

public class BigDecimalValueType extends BigDecimalType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new BigDecimalValue(reader.getValue().trim());
    }
}