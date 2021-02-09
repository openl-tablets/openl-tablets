package org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.basic.BeanType;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.cxf.aegis.xml.MessageReader;
import org.openl.rules.helpers.DoubleRange;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * Defines DoubleRange deserialization from XML because it has no default constuctor.
 *
 * @author PUdalau
 */
public class DoubleRangeBeanType extends BeanType {

    public static final Class<?> TYPE_CLASS = DoubleRange.class;

    public static final QName QNAME = new Java5TypeCreator().createQName(TYPE_CLASS);

    public DoubleRangeBeanType() {
        super();
        setTypeClass(TYPE_CLASS);
        setSchemaType(QNAME);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        try {

            double lowerBound = 0;
            double upperBound = 0;
            BoundType lowerBoundType = BoundType.INCLUDING;
            BoundType upperBoundType = BoundType.INCLUDING;
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
                } else if (propertyName.equals("lowerBound")) {
                    lowerBound = childReader.getValueAsDouble();
                } else if (propertyName.equals("upperBound")) {
                    upperBound = childReader.getValueAsDouble();
                } else if (propertyName.equals("lowerBoundType")) {
                    lowerBoundType = BoundType.valueOf(childReader.getValue().trim().toUpperCase());
                } else if (propertyName.equals("upperBoundType")) {
                    upperBoundType = BoundType.valueOf(childReader.getValue().trim().toUpperCase());
                }
            }

            return new DoubleRange(lowerBound, upperBound, lowerBoundType, upperBoundType);
        } catch (Exception e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }
}
