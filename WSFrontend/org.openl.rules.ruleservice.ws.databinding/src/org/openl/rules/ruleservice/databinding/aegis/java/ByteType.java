package org.openl.rules.ruleservice.databinding.aegis.java;

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
public class ByteType extends org.apache.cxf.aegis.type.basic.ByteType {

    @Override
    public Object readObject(MessageReader reader, Context context) {
        String value = reader.getValue();
        if (value == null) {
            return null;
        }
        if (value.trim().isEmpty()) {
            return null;
        }

        return Byte.valueOf(value);
    }

}
