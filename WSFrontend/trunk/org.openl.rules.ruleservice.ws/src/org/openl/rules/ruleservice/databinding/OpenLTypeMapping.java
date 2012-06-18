package org.openl.rules.ruleservice.databinding;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.project.instantiation.variation.VariationsResult;
import org.openl.rules.ruleservice.context.ArgumentReplacementVariationType;
import org.openl.rules.ruleservice.context.BigDecimalValueType;
import org.openl.rules.ruleservice.context.BigIntegerValueType;
import org.openl.rules.ruleservice.context.ByteValueType;
import org.openl.rules.ruleservice.context.ComplexVariationType;
import org.openl.rules.ruleservice.context.DeepCloningVariationType;
import org.openl.rules.ruleservice.context.DoubleRangeBeanType;
import org.openl.rules.ruleservice.context.DoubleValueType;
import org.openl.rules.ruleservice.context.FloatValueType;
import org.openl.rules.ruleservice.context.IntRangeBeanType;
import org.openl.rules.ruleservice.context.IntValueType;
import org.openl.rules.ruleservice.context.JXPathVariationType;
import org.openl.rules.ruleservice.context.LongValueType;
import org.openl.rules.ruleservice.context.RuntimeContextBeanType;
import org.openl.rules.ruleservice.context.ShortValueType;
import org.openl.rules.ruleservice.context.VariationsResultType;

/**
 * OpenL implementation of {@link TypeMapping} with custom mappings for specific
 * OpenL classes.
 * 
 * It was introduced because of problem of one issue in Aegis Databinding: Type
 * mapping works with java {@link Type} that is more common that {@link Class},
 * so if we can not define custom binding for some generic type independently of
 * generic type(in this case we should register custom binding for each
 * particular {@link ParameterizedType}).
 * 
 * This problem have been encountered for {@link VariationsResult} type. So for
 * this class we return custom binding independently of its generic
 * type(otherwise we should register this mapping explicitly in server and
 * client).
 * 
 * @author PUdalau
 * 
 */
public class OpenLTypeMapping extends DefaultTypeMapping {
    public OpenLTypeMapping(TypeMapping baseTM) {
        super(DEFAULT_MAPPING_URI, baseTM);
        register(new RuntimeContextBeanType());
        register(new VariationsResultType());
        register(new JXPathVariationType());
        register(new ArgumentReplacementVariationType());
        register(new DeepCloningVariationType());
        register(new ComplexVariationType());
        register(new IntRangeBeanType());
        register(new DoubleRangeBeanType());
        register(ShortValue.class, XMLSchemaQNames.XSD_SHORT, new ShortValueType());
        register(LongValue.class, XMLSchemaQNames.XSD_LONG, new LongValueType());
        register(IntValue.class, XMLSchemaQNames.XSD_INT, new IntValueType());
        register(FloatValue.class, XMLSchemaQNames.XSD_FLOAT, new FloatValueType());
        register(DoubleValue.class, XMLSchemaQNames.XSD_DOUBLE, new DoubleValueType());
        register(ByteValue.class, XMLSchemaQNames.XSD_BYTE, new ByteValueType());
        register(BigIntegerValue.class, XMLSchemaQNames.XSD_INTEGER, new BigIntegerValueType());
        register(BigDecimalValue.class, XMLSchemaQNames.XSD_DECIMAL, new BigDecimalValueType());
    }

    @Override
    public AegisType getType(Type javaType) {
        if (javaType instanceof ParameterizedType && ((ParameterizedType) javaType).getRawType() == VariationsResult.class) {
            return getType(VariationsResult.class);
        }
        return super.getType(javaType);
    }

    @Override
    public AegisType getType(QName xmlType) {
        return super.getType(xmlType);
    }

    @Override
    public TypeCreator getTypeCreator() {
        TypeCreator typeCreator = super.getTypeCreator();
        if (typeCreator == null && getParent() != null) {
            typeCreator = getParent().getTypeCreator();
        }
        if (typeCreator != null) {
            typeCreator.setTypeMapping(this);
        }
        return typeCreator;
    }

}
