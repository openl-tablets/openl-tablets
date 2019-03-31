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
import org.apache.cxf.binding.corba.wsdl.W3CConstants;

public class AegisDatabindingFactoryBean extends AbstractAegisDatabindingFactoryBean{ 

    @Override
    protected void registerOpenLTypes(TypeMapping typeMapping) {
		loadAegisTypeClassAndRegister("org.openl.meta.StringValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.StringValueType.class, W3CConstants.NT_SCHEMA_STRING, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.ShortValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.ShortValueType.class, W3CConstants.NT_SCHEMA_SHORT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.LongValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.LongValueType.class, W3CConstants.NT_SCHEMA_LONG, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.IntValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.IntValueType.class, W3CConstants.NT_SCHEMA_INT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.FloatValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.FloatValueType.class, W3CConstants.NT_SCHEMA_FLOAT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.DoubleValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.DoubleValueType.class, W3CConstants.NT_SCHEMA_DOUBLE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.ByteValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.ByteValueType.class, W3CConstants.NT_SCHEMA_BYTE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigIntegerValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.BigIntegerValueType.class, W3CConstants.NT_SCHEMA_INTEGER, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigDecimalValue",
                org.openl.rules.ruleservice.databinding.aegis.org.openl.meta.BigDecimalValueType.class, W3CConstants.NT_SCHEMA_DECIMAL, typeMapping);
	}

	@Override
    protected void registerCustomJavaTypes(TypeMapping typeMapping) {
		//CUSTOM JAVA TYPES REGISTER
        loadAegisTypeClassAndRegister(Character.class, CharacterAsStringType.class, W3CConstants.NT_SCHEMA_INT, typeMapping);
        loadAegisTypeClassAndRegister(char.class, CharacterAsStringType.class, W3CConstants.NT_SCHEMA_INT, typeMapping);
        //END
	}

}
