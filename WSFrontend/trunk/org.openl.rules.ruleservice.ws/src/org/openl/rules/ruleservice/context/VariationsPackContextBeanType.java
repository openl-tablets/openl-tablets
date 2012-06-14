package org.openl.rules.ruleservice.context;

import java.util.List;
import java.util.Map;

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
import org.openl.rules.project.instantiation.variation.Variation;
import org.openl.rules.project.instantiation.variation.VariationsPack;

/**
 *FIXME
 * Defines IRulesRuntime context deserialization from XML: new
 * {@link DefaultRulesRuntimeContext} will be used(By default Aegis creates
 * Proxy that does not provide some necessary methods, e.g. <code>clone()</code>
 * ).
 * 
 * @author PUdalau
 */
public class VariationsPackContextBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = Map.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public VariationsPackContextBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo inf = getTypeInfo();

        try {
            VariationsPack variationsPack = null;
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
                if (type != null && qName.getLocalPart().equals("variations")) {
                    Object variations = type.readObject(childReader, context);
                    variationsPack = new VariationsPack((List<Variation>) variations);//FIXME
                } else {
                    childReader.readToEnd();
                }
            }

            return variationsPack;
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
