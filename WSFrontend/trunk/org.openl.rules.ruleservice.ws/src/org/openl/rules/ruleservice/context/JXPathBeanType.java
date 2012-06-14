package org.openl.rules.ruleservice.context;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.project.instantiation.variation.JXPathVariation;
import org.openl.rules.project.instantiation.variation.VariationsFactory;

/**
 *FIXME
 * Defines IRulesRuntime context deserialization from XML: new
 * {@link DefaultRulesRuntimeContext} will be used(By default Aegis creates
 * Proxy that does not provide some necessary methods, e.g. <code>clone()</code>
 * ).
 * 
 * @author PUdalau
 */
public class JXPathBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = JXPathVariation.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public JXPathBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            String variationID = "";
            int updatedArgumentIndex = 0;
            String path = VariationsFactory.THIS_POINTER;
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
                AegisType type = TypeUtil.getReadType(childReader.getXMLStreamReader(),
                    context.getGlobalContext(),
                    defaultType);
                if (type != null) {
                    String propertyName = qName.getLocalPart();
                    Object propertyValue = type.readObject(childReader, context);
                    if("variationID".equals(propertyName)){
                        variationID = String.valueOf(propertyValue);
                    }else if("updatedArgumentIndex".equals(propertyName)){
                        //FIXME check
                        updatedArgumentIndex = (Integer) propertyValue;
                    }else if("path".equals(propertyName)){
                        path = String.valueOf(propertyValue);
                    }else if("valueToSet".equals(propertyName)){
                        valueToSet = propertyValue;
                        
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return new JXPathVariation(variationID, updatedArgumentIndex, path, valueToSet);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
