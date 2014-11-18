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


import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.databinding.aegis.java.BigDecimalType;
import org.openl.rules.ruleservice.databinding.aegis.java.BigIntegerType;
import org.openl.rules.ruleservice.databinding.aegis.java.ByteType;
import org.openl.rules.ruleservice.databinding.aegis.java.DoubleType;
import org.openl.rules.ruleservice.databinding.aegis.java.FloatType;
import org.openl.rules.ruleservice.databinding.aegis.java.IntType;
import org.openl.rules.ruleservice.databinding.aegis.java.LongType;
import org.openl.rules.ruleservice.databinding.aegis.java.ShortType;
import org.openl.rules.table.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AegisDatabindingFactoryBean {

    private final Logger log = LoggerFactory.getLogger(AegisDatabindingFactoryBean.class);

    private Boolean writeXsiTypes;
    private Boolean readXsiTypes;
    private Set<String> overrideTypes;
    private TypeCreationOptions configuration;
    private Boolean mtomUseXmime;
    private Boolean mtomEnabled;
    private Integer mtomThreshold;
    private Bus bus;
    private Collection<DOMSource> schemas;
    private Map<String, String> namespaceMap;
    private boolean supportVariations = false;

    public AegisDatabinding createAegisDatabinding() {
        AegisDatabinding aegisDatabinding = new AegisDatabinding();
        if (getConfiguration() != null) {
            aegisDatabinding.setConfiguration(configuration);
        }

        if (getMtomUseXmime() != null) {
            aegisDatabinding.setMtomUseXmime(getMtomUseXmime().booleanValue());
        }

        if (getMtomEnabled() != null) {
            aegisDatabinding.setMtomEnabled(getMtomEnabled().booleanValue());
        }

        if (getBus() != null) {
            aegisDatabinding.setBus(getBus());
        }

        if (getMtomThreshold() != null) {
            aegisDatabinding.setMtomThreshold(getMtomThreshold().intValue());
        }

        if (getNamespaceMap() != null) {
            aegisDatabinding.setNamespaceMap(getNamespaceMap());
        }

        if (getSchemas() != null) {
            aegisDatabinding.setSchemas(getSchemas());
        }

        aegisDatabinding.setOverrideTypes(getOverrideTypesWithDefaultOpenLTypes());

        if (getWriteXsiTypes() != null) {
            aegisDatabinding.getAegisContext().setWriteXsiTypes(getWriteXsiTypes().booleanValue());
        }

        if (getReadXsiTypes() != null) {
            aegisDatabinding.getAegisContext().setReadXsiTypes(getReadXsiTypes().booleanValue());
        }

        TypeMapping typeMapping = aegisDatabinding.getAegisContext().getTypeMapping();
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.context.RuntimeContextBeanType.class, typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.RuleServiceRuntimeContextBeanType.class,
                typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.calc.SpreadSheetResultType.class, typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.table.PointType.class, typeMapping);

        if (supportVariations) {
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.VariationsResultType.class, typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.JXPathVariationType.class, typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ArgumentReplacementVariationType.class,
                    typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.DeepCloningVariationType.class,
                    typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ComplexVariationType.class, typeMapping);
        }
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper.IntRangeBeanType.class, typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper.DoubleRangeBeanType.class, typeMapping);

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

        return aegisDatabinding;
    }

    protected void loadAegisTypeClassAndRegister(String aegisTypeClassName, TypeMapping typeMapping) {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(aegisTypeClassName);
            Constructor<?> constructor = clazz.getConstructor();
            AegisType aegisType = (AegisType) constructor.newInstance();
            typeMapping.register(aegisType);
        } catch (Exception e) {
            log.warn("Aegis type \"{}\" registration failed!", aegisTypeClassName, e);
        }
    }

    protected void loadAegisTypeClassAndRegister(Class<?> aegisTypeClass, TypeMapping typeMapping) {
        try {
            Constructor<?> constructor = aegisTypeClass.getConstructor();
            AegisType aegisType = (AegisType) constructor.newInstance();
            typeMapping.register(aegisType);
        } catch (Exception e) {
            log.warn("Aegis type \"{}\" registration failed!", aegisTypeClass.getName(), e);
        }
    }

    protected void loadAegisTypeClassAndRegister(String typeClassName, Class<?> aegisTypeClass, QName qName,
                                                 TypeMapping typeMapping) {
        try {
            Class<?> typeClazz = Thread.currentThread().getContextClassLoader().loadClass(typeClassName);
            Constructor<?> constructor = aegisTypeClass.getConstructor();
            AegisType aegisType = (AegisType) constructor.newInstance();
            typeMapping.register(typeClazz, qName, aegisType);
        } catch (Exception e) {
            log.warn("Type \"{}\" registration failed!", typeClassName, e);
        }
    }

    protected Set<String> getOverrideTypesWithDefaultOpenLTypes() {
        Set<String> overrideTypes = new HashSet<String>();
        if (getOverrideTypes() != null) {
            overrideTypes.addAll(getOverrideTypes());
        }
        overrideTypes.add(SpreadsheetResult.class.getCanonicalName());
        overrideTypes.add(Point.class.getCanonicalName());

        if (supportVariations) {
            overrideTypes.add("org.openl.rules.variation.VariationsResult");
            overrideTypes.add("org.openl.rules.variation.Variation");
            overrideTypes.add("org.openl.rules.variation.ComplexVariation");
            overrideTypes.add("org.openl.rules.variation.NoVariation");
            overrideTypes.add("org.openl.rules.variation.JXPathVariation");
            overrideTypes.add("org.openl.rules.variation.DeepCloningVariation");
            overrideTypes.add("org.openl.rules.variation.ArgumentReplacementVariation");
        }
        return overrideTypes;
    }

    public Boolean getWriteXsiTypes() {
        return writeXsiTypes;
    }

    public void setWriteXsiTypes(Boolean writeXsiTypes) {
        this.writeXsiTypes = writeXsiTypes;
    }

    public Set<String> getOverrideTypes() {
        return overrideTypes;
    }

    public void setOverrideTypes(Set<String> overrideTypes) {
        this.overrideTypes = overrideTypes;
    }

    public TypeCreationOptions getConfiguration() {
        return configuration;
    }

    public void setConfiguration(TypeCreationOptions configuration) {
        this.configuration = configuration;
    }

    public void setMtomUseXmime(Boolean mtomUseXmime) {
        this.mtomUseXmime = mtomUseXmime;
    }

    public Boolean getMtomUseXmime() {
        return mtomUseXmime;
    }

    public Boolean getMtomEnabled() {
        return mtomEnabled;
    }

    public void setMtomEnabled(Boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public Integer getMtomThreshold() {
        return mtomThreshold;
    }

    public void setMtomThreshold(Integer mtomThreshold) {
        this.mtomThreshold = mtomThreshold;
    }

    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }

    public Collection<DOMSource> getSchemas() {
        return schemas;
    }

    public void setSchemas(Collection<DOMSource> schemas) {
        this.schemas = schemas;
    }

    public Map<String, String> getNamespaceMap() {
        return namespaceMap;
    }

    public void setNamespaceMap(Map<String, String> namespaceMap) {
        this.namespaceMap = namespaceMap;
    }

    public boolean isSupportVariations() {
        return supportVariations;
    }

    public void setSupportVariations(boolean supportVariations) {
        this.supportVariations = supportVariations;
    }
    
    public Boolean getReadXsiTypes() {
        return readXsiTypes;
    }
    
    public void setReadXsiTypes(Boolean readXsiTypes) {
        this.readXsiTypes = readXsiTypes;
    }
}
