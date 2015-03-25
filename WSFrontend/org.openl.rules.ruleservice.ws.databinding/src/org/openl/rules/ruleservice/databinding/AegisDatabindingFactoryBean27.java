package org.openl.rules.ruleservice.databinding;

/*
 * #%L
 * OpenL - RuleService - RuleService - Web Services Databinding
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import java.math.BigDecimal;
import java.math.BigInteger;

import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.openl.rules.ruleservice.databinding.aegis.java.BigDecimalType;
import org.openl.rules.ruleservice.databinding.aegis.java.BigIntegerType;
import org.openl.rules.ruleservice.databinding.aegis.java.ByteType;
import org.openl.rules.ruleservice.databinding.aegis.java.DoubleType;
import org.openl.rules.ruleservice.databinding.aegis.java.FloatType;
import org.openl.rules.ruleservice.databinding.aegis.java.IntType;
import org.openl.rules.ruleservice.databinding.aegis.java.LongType;
import org.openl.rules.ruleservice.databinding.aegis.java.ShortType;

public class AegisDatabindingFactoryBean27 extends AbstractAegisDatabindingFactoryBean{ 
    
    protected void registerOpenLTypes(TypeMapping typeMapping) {
        loadAegisTypeClassAndRegister("org.openl.meta.StringValue",
            org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.StringValueType.class, XMLSchemaQNames.XSD_STRING, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.ShortValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.ShortValueType.class, XMLSchemaQNames.XSD_SHORT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.LongValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.LongValueType.class, XMLSchemaQNames.XSD_LONG, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.IntValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.IntValueType.class, XMLSchemaQNames.XSD_INT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.FloatValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.FloatValueType.class, XMLSchemaQNames.XSD_FLOAT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.DoubleValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.DoubleValueType.class, XMLSchemaQNames.XSD_DOUBLE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.ByteValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.ByteValueType.class, XMLSchemaQNames.XSD_BYTE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigIntegerValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.BigIntegerValueType.class, XMLSchemaQNames.XSD_INTEGER, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigDecimalValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.BigDecimalValueType.class, XMLSchemaQNames.XSD_DECIMAL, typeMapping);
    }

    protected void registerCustomJavaTypes(TypeMapping typeMapping) {
        //CUSTOM JAVA TYPES REGISTER
        loadAegisTypeClassAndRegister(Double.class.getCanonicalName(),
            DoubleType.class, XMLSchemaQNames.XSD_DOUBLE, typeMapping);
        loadAegisTypeClassAndRegister(Float.class.getCanonicalName(),
            FloatType.class, XMLSchemaQNames.XSD_FLOAT, typeMapping);
        loadAegisTypeClassAndRegister(Integer.class.getCanonicalName(),
            IntType.class, XMLSchemaQNames.XSD_INT, typeMapping);
        loadAegisTypeClassAndRegister(Long.class.getCanonicalName(),
            LongType.class, XMLSchemaQNames.XSD_LONG, typeMapping);
        loadAegisTypeClassAndRegister(Short.class.getCanonicalName(),
            ShortType.class, XMLSchemaQNames.XSD_SHORT, typeMapping);
        loadAegisTypeClassAndRegister(Byte.class.getCanonicalName(),
            ByteType.class, XMLSchemaQNames.XSD_BYTE, typeMapping);
        loadAegisTypeClassAndRegister(BigInteger.class.getCanonicalName(),
            BigIntegerType.class, XMLSchemaQNames.XSD_INTEGER, typeMapping);
        loadAegisTypeClassAndRegister(BigDecimal.class.getCanonicalName(),
            BigDecimalType.class, XMLSchemaQNames.XSD_DECIMAL, typeMapping);
        //END
    }
}
