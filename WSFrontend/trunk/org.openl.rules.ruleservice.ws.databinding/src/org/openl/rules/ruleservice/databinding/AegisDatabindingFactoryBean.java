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
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.util.XMLSchemaQNames;

public class AegisDatabindingFactoryBean {

    private final Log log = LogFactory.getLog(AegisDatabindingFactoryBean.class);

    private Boolean writeXsiTypes;
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

        TypeMapping typeMapping = aegisDatabinding.getAegisContext().getTypeMapping();
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.RuntimeContextBeanType.class, typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.RuleServiceRuntimeContextBeanType.class,
                typeMapping);
        if (supportVariations) {
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.VariationsResultType.class, typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.JXPathVariationType.class, typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.ArgumentReplacementVariationType.class,
                    typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.DeepCloningVariationType.class,
                    typeMapping);
            loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.ComplexVariationType.class, typeMapping);
        }
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.IntRangeBeanType.class, typeMapping);
        loadAegisTypeClassAndRegister(org.openl.rules.ruleservice.context.DoubleRangeBeanType.class, typeMapping);

        loadAegisTypeClassAndRegister("org.openl.meta.ShortValue",
                org.openl.rules.ruleservice.context.ShortValueType.class, XMLSchemaQNames.XSD_SHORT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.LongValue",
                org.openl.rules.ruleservice.context.LongValueType.class, XMLSchemaQNames.XSD_LONG, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.IntValue",
                org.openl.rules.ruleservice.context.IntValueType.class, XMLSchemaQNames.XSD_INT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.FloatValue",
                org.openl.rules.ruleservice.context.FloatValueType.class, XMLSchemaQNames.XSD_FLOAT, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.DoubleValue",
                org.openl.rules.ruleservice.context.DoubleValueType.class, XMLSchemaQNames.XSD_DOUBLE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.ByteValue",
                org.openl.rules.ruleservice.context.ByteValueType.class, XMLSchemaQNames.XSD_BYTE, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigIntegerValue",
                org.openl.rules.ruleservice.context.BigIntegerValueType.class, XMLSchemaQNames.XSD_INTEGER, typeMapping);
        loadAegisTypeClassAndRegister("org.openl.meta.BigDecimalValue",
                org.openl.rules.ruleservice.context.BigDecimalValueType.class, XMLSchemaQNames.XSD_DECIMAL, typeMapping);

        return aegisDatabinding;
    }

    protected void loadAegisTypeClassAndRegister(String aegisTypeClassName, TypeMapping typeMapping) {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(aegisTypeClassName);
            Constructor<?> constructor = clazz.getConstructor();
            AegisType aegisType = (AegisType) constructor.newInstance();
            typeMapping.register(aegisType);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Aegis type \"" + aegisTypeClassName + "\" registration failed!", e);
            }
        }
    }

    protected void loadAegisTypeClassAndRegister(Class<?> aegisTypeClass, TypeMapping typeMapping) {
        try {
            Constructor<?> constructor = aegisTypeClass.getConstructor();
            AegisType aegisType = (AegisType) constructor.newInstance();
            typeMapping.register(aegisType);
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Aegis type \"" + aegisTypeClass.getName() + "\" registration failed!", e);
            }
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
            if (log.isWarnEnabled()) {
                log.warn("Type \"" + typeClassName + "\" registration failed!", e);
            }
        }
    }

    protected Set<String> getOverrideTypesWithDefaultOpenLTypes() {
        Set<String> overrideTypes = new HashSet<String>();
        if (getOverrideTypes() != null) {
            overrideTypes.addAll(getOverrideTypes());
        }
        if (supportVariations) {
            overrideTypes.add("org.openl.rules.variation.VariationsResult");
            overrideTypes.add("org.openl.rules.variation.Variation");
            overrideTypes.add("org.openl.rules.variation.ComplexVariation");
            overrideTypes.add("org.openl.rules.variation.NoVariation");
            overrideTypes.add("org.openl.rules.variation.JXPathVariation");
            overrideTypes.add("org.openl.rules.variation.DeepCloningVariaion");
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
}
