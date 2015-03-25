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
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.openl.rules.ruleservice.databinding.aegis.java.BigDecimalType;
import org.openl.rules.ruleservice.databinding.aegis.java.BigIntegerType;
import org.openl.rules.ruleservice.databinding.aegis.java.ByteType;
import org.openl.rules.ruleservice.databinding.aegis.java.DoubleType;
import org.openl.rules.ruleservice.databinding.aegis.java.FloatType;
import org.openl.rules.ruleservice.databinding.aegis.java.IntType;
import org.openl.rules.ruleservice.databinding.aegis.java.LongType;
import org.openl.rules.ruleservice.databinding.aegis.java.ShortType;

public class AegisDatabindingFactoryBean extends AbstractAegisDatabindingFactoryBean{ 

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

	protected void registerCustomJavaTypes(TypeMapping typeMapping) {
		//CUSTOM JAVA TYPES REGISTER
        loadAegisTypeClassAndRegister(Double.class.getCanonicalName(),
            DoubleType.class, W3CConstants.NT_SCHEMA_DOUBLE, typeMapping);
        loadAegisTypeClassAndRegister(Float.class.getCanonicalName(),
            FloatType.class, W3CConstants.NT_SCHEMA_FLOAT, typeMapping);
        loadAegisTypeClassAndRegister(Integer.class.getCanonicalName(),
            IntType.class, W3CConstants.NT_SCHEMA_INT, typeMapping);
        loadAegisTypeClassAndRegister(Long.class.getCanonicalName(),
            LongType.class, W3CConstants.NT_SCHEMA_LONG, typeMapping);
        loadAegisTypeClassAndRegister(Short.class.getCanonicalName(),
            ShortType.class, W3CConstants.NT_SCHEMA_SHORT, typeMapping);
        loadAegisTypeClassAndRegister(Byte.class.getCanonicalName(),
            ByteType.class, W3CConstants.NT_SCHEMA_BYTE, typeMapping);
        loadAegisTypeClassAndRegister(BigInteger.class.getCanonicalName(),
            BigIntegerType.class, W3CConstants.NT_SCHEMA_INTEGER, typeMapping);
        loadAegisTypeClassAndRegister(BigDecimal.class.getCanonicalName(),
            BigDecimalType.class, W3CConstants.NT_SCHEMA_DECIMAL, typeMapping);
        //END
	}

}
