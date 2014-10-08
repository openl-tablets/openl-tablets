package org.openl.rules.ruleservice.databinding.aegis.custom;

import java.math.BigDecimal;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.xml.MessageReader;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */
public class BigDecimalType extends org.apache.cxf.aegis.type.basic.BigDecimalType {

    @Override
    public Object readObject(MessageReader reader, Context context) {
        String value = reader.getValue();
        if (value == null) {
            return null;
        }
        if (value.trim().isEmpty()) {
            return null;
        }

        return new BigDecimal(value);
    }

}
