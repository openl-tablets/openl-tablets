package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.variation.ArgumentReplacementVariation;

/**
 * Custom mapping for {@link ArgumentReplacementVariationType} due to it is not usual bean and should be initialized
 * through non-default constructor.
 *
 * @author PUdalau
 */
public class ArgumentReplacementVariationType extends BeanType {

    public static final Class<?> TYPE_CLASS = ArgumentReplacementVariation.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public ArgumentReplacementVariationType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        BeanTypeInfo inf = getTypeInfo();

        try {
            String variationID = "";
            int updatedArgumentIndex = 0;
            Object valueToSet = null;
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
                if (type != null) {
                    String propertyName = qName.getLocalPart();
                    Object propertyValue = type.readObject(childReader, context);
                    if ("variationID".equals(propertyName)) {
                        variationID = String.valueOf(propertyValue);
                    } else if ("updatedArgumentIndex".equals(propertyName)) {
                        updatedArgumentIndex = (Integer) propertyValue;
                    } else if ("valueToSet".equals(propertyName)) {
                        valueToSet = propertyValue;
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return new ArgumentReplacementVariation(variationID, updatedArgumentIndex, valueToSet);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
