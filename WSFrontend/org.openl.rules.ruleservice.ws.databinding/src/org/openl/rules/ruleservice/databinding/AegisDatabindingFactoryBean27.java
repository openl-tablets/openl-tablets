package org.openl.rules.ruleservice.databinding;

/*-
 * #%L
 * OpenL - RuleService - Web Services - Databinding
 * %%
 * Copyright (C) 2015 - 2018 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */


import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.basic.CharacterAsStringType;
import org.apache.cxf.common.util.XMLSchemaQNames;

public class AegisDatabindingFactoryBean27 extends AbstractAegisDatabindingFactoryBean { 
    
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
        loadAegisTypeClassAndRegister(Character.class, CharacterAsStringType.class, XMLSchemaQNames.XSD_INT, typeMapping);
        loadAegisTypeClassAndRegister(char.class, CharacterAsStringType.class, XMLSchemaQNames.XSD_INT, typeMapping);
        //END
    }
}
