package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.ruleservice.databinding.aegis.WrapperBeanTypeInfo;
import org.openl.rules.variation.VariationsResult;

/**
 * Custom mapping for {@link VariationsResultType} due to it is not usual bean all results should be registered using
 * the special methods.
 * 
 * This class uses Java Generics and causes one problems that is described in {@link OpenLTypeMapping}.
 * 
 * @author PUdalau
 */
public class VariationsResultType extends BeanType {
    public static final Class<?> TYPE_CLASS = VariationsResult.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public VariationsResultType() {
        super(new WrapperBeanTypeInfo(TYPE_CLASS,
            QNAME.getNamespaceURI(),
            Arrays.asList("allProcessedVariationIDs", "calculatedVariationIDs", "failedVariationIDs")));
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object readObject(MessageReader reader, Context context) {
        BeanTypeInfo inf = getTypeInfo();

        try {
            VariationsResult variationsResult = new VariationsResult();
            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName qName = childReader.getName();
                AegisType defaultType = inf.getType(qName);
                AegisType type = TypeUtil
                    .getReadType(childReader.getXMLStreamReader(), context.getGlobalContext(), defaultType);
                if (type != null && qName.getLocalPart().equals("variationFailures")) {
                    Map<String, String> variationFailures = (Map<String, String>) type.readObject(childReader, context);
                    for (Entry<String, String> failure : variationFailures.entrySet()) {
                        variationsResult.registerFailure(failure.getKey(), failure.getValue());
                    }
                } else if (type != null && qName.getLocalPart().equals("variationResults")) {
                    Map<String, Object> variationResults = (Map<String, Object>) type.readObject(childReader, context);
                    for (Entry<String, Object> result : variationResults.entrySet()) {
                        variationsResult.registerResult(result.getKey(), result.getValue());
                    }
                } else if (type != null && qName.getLocalPart().equals("data")) {
                    byte[] data = (byte[]) type.readObject(childReader, context);
                    variationsResult.setData(data);
                } else {
                    childReader.readToEnd();
                }
            }

            return variationsResult;
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
