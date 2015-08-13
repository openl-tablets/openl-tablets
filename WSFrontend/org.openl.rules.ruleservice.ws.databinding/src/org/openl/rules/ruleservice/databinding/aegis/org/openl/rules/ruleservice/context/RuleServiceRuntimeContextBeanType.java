package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.ruleservice.context;

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
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.ruleservice.context.DefaultRulesRuntimeContext;
import org.openl.rules.ruleservice.context.IRulesRuntimeContext;

/**
 * Defines IRulesRuntime (from Rule Service project) context deserialization from XML: new
 * {@link DefaultRulesRuntimeContext} will be used(By default Aegis creates
 * Proxy that does not provide some necessary methods, e.g. <code>clone()</code>
 * ).
 * 
 * @author Marat Kamalov
 */
public class RuleServiceRuntimeContextBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = IRulesRuntimeContext.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    private static BeanTypeInfo getBeanTypeInfo(){
        BeanTypeInfo bti = new BeanTypeInfo(TYPE_CLASS, QNAME.getNamespaceURI());
        bti.setExtensibleAttributes(false);
        bti.setExtensibleElements(false);
        return bti;
    }
    
    public RuleServiceRuntimeContextBeanType() {
        super(getBeanTypeInfo());
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            DefaultRulesRuntimeContext runtimeContext = new DefaultRulesRuntimeContext();
            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                if (childReader.isXsiNil()) {
                    childReader.readToEnd();
                    continue;
                }
                QName qName = childReader.getName();
                AegisType defaultType = inf.getType(qName);
                AegisType type = TypeUtil.getReadType(childReader.getXMLStreamReader(),
                    context.getGlobalContext(),
                    defaultType);
                if (type != null) {
                    String propertyName = qName.getLocalPart();
                    Object propertyValue = type.readObject(childReader, context);
                    runtimeContext.setValue(propertyName, propertyValue);
                } else {
                    childReader.readToEnd();
                }
            }

            return runtimeContext;
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
