package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.table;

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
import org.openl.rules.table.Point;

/**
 * Custom mapping for {@link Point} due to it is not usual bean all
 * results should be registered using the special methods.
 * 
 * This class uses Java Generics and causes one problems that is described in
 * {@link OpenLTypeMapping}.
 * 
 * @author Marat Kamalov
 */
public class PointType extends BeanType {
    public static final Class<?> TYPE_CLASS = Point.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    private static BeanTypeInfo getBeanTypeInfo(){
        BeanTypeInfo bti = new BeanTypeInfo(TYPE_CLASS, QNAME.getNamespaceURI());
        bti.setExtensibleAttributes(false);
        bti.setExtensibleElements(false);
        return bti;
    }
    
    public PointType() {
        super(getBeanTypeInfo());
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            int column = 0;
            int row = 0;
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
                if (type != null && qName.getLocalPart().equals("column")) {
                    column = (Integer) type.readObject(childReader, context);
                } else if (type != null && qName.getLocalPart().equals("row")) {
                    row = (Integer) type.readObject(childReader, context);
                } else {
                    childReader.readToEnd();
                }
            }

            return new Point(column, row);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}