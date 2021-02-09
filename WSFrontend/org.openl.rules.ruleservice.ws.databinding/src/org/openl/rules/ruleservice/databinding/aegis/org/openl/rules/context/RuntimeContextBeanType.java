package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.context;

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
import org.openl.rules.context.RulesRuntimeContextFactory;

/**
 * Defines IRulesRuntime context deserialization from XML: new {@link DefaultRulesRuntimeContext} will be used(By
 * default Aegis creates Proxy that does not provide some necessary methods, e.g. <code>clone()</code> ).
 *
 * @author PUdalau
 */
public class RuntimeContextBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = IRulesRuntimeContext.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    private static BeanTypeInfo getBeanTypeInfo() {
        BeanTypeInfo bti = new BeanTypeInfo(TYPE_CLASS, QNAME.getNamespaceURI());
        bti.setExtensibleAttributes(false);
        bti.setExtensibleElements(false);
        return bti;
    }

    public RuntimeContextBeanType() {
        super(getBeanTypeInfo());
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        BeanTypeInfo inf = getTypeInfo();

        try {
            IRulesRuntimeContext runtimeContext = RulesRuntimeContextFactory.buildRulesRuntimeContext();
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
