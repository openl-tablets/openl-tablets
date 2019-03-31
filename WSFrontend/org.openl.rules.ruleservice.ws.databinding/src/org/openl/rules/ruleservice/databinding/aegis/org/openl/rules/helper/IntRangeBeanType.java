package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.helpers.IntRange;

/**
 * Defines IntRange deserialization from XML because it has no default constuctor.
 * 
 * @author PUdalau
 */
public class IntRangeBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = IntRange.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public IntRangeBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        try {

            int min = 0;
            int max = 0;
            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName qName = childReader.getName();
                String propertyName = qName.getLocalPart();
                if (propertyName == null) {
                    childReader.readToEnd();
                } else if (propertyName.equals("min")) {
                    min = childReader.getValueAsInt();
                } else if (propertyName.equals("max")) {
                    max = childReader.getValueAsInt();
                }
            }

            return new IntRange(min, max);
        } catch (Exception e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
