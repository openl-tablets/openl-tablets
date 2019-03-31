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
import java.lang.reflect.InvocationTargetException;
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
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAegisDatabindingFactoryBean {

    private final Logger log = LoggerFactory.getLogger(AbstractAegisDatabindingFactoryBean.class);

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
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.context.RuntimeContextBeanType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.calc.SpreadsheetResultType.class,
            typeMapping);
       
        if (supportVariations) {
            registerVariationTypes(typeMapping);
        }
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper.IntRangeBeanType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.helper.DoubleRangeBeanType.class,
            typeMapping);

        registerCustomJavaTypes(typeMapping);

        registerOpenLTypes(typeMapping);
        
        return aegisDatabinding;
    }

    protected void registerVariationTypes(TypeMapping typeMapping) {
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.VariationsResultType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.JXPathVariationType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ArgumentReplacementVariationType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.DeepCloningVariationType.class,
            typeMapping);
        loadAegisTypeClassAndRegister(
            org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ComplexVariationType.class,
            typeMapping);
    }

    protected abstract void registerOpenLTypes(TypeMapping typeMapping);

    protected abstract void registerCustomJavaTypes(TypeMapping typeMapping);

    protected void loadAegisTypeClassAndRegister(String aegisTypeClassName, TypeMapping typeMapping) {
        try {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(aegisTypeClassName);
            AegisType aegisType = instatiateAegisType(clazz);
            typeMapping.register(aegisType);
        } catch (Exception e) {
            log.warn("Aegis type \"{}\" registration failed!", aegisTypeClassName, e);
        }
    }

    private AegisType instatiateAegisType(Class<?> clazz) throws NoSuchMethodException,
                                                          InstantiationException,
                                                          IllegalAccessException,
                                                          InvocationTargetException {
        Constructor<?> constructor = clazz.getConstructor();
        return (AegisType) constructor.newInstance();
    }

    protected void loadAegisTypeClassAndRegister(Class<?> aegisTypeClass, TypeMapping typeMapping) {
        try {
            AegisType aegisType = instatiateAegisType(aegisTypeClass);
            typeMapping.register(aegisType);
        } catch (Exception e) {
            log.warn("Aegis type \"{}\" registration failed!", aegisTypeClass.getName(), e);
        }
    }

    protected void loadAegisTypeClassAndRegister(String typeClassName,
            Class<?> aegisTypeClass,
            QName qName,
            TypeMapping typeMapping) {
        try {
            Class<?> typeClazz = Thread.currentThread().getContextClassLoader().loadClass(typeClassName);
            AegisType aegisType = instatiateAegisType(aegisTypeClass);
            typeMapping.register(typeClazz, qName, aegisType);
        } catch (Exception e) {
            log.warn("Type \"{}\" registration failed!", typeClassName, e);
        }
    }

    protected void loadAegisTypeClassAndRegister(Class<?> typeClazz,
            Class<?> aegisTypeClass,
            QName qName,
            TypeMapping typeMapping) {
        try {
            AegisType aegisType = instatiateAegisType(aegisTypeClass);
            typeMapping.register(typeClazz, qName, aegisType);
        } catch (Exception e) {
            log.warn("Type \"{}\" registration failed!", typeClazz.getName(), e);
        }
    }

    private void tryToLoadClass(String className) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            log.warn("Class '{}' hasn't been found!", className, e);
        }
    }
    
    protected Set<String> getOverrideTypesWithDefaultOpenLTypes() {
        Set<String> overrideTypes = new HashSet<>();
        if (getOverrideTypes() != null) {
            for (String className : getOverrideTypes()) {
                tryToLoadClass(className);
            }
            overrideTypes.addAll(getOverrideTypes());
        }
        overrideTypes.add(SpreadsheetResult.class.getCanonicalName());
        overrideTypes.add(Point.class.getCanonicalName());

        if (supportVariations) {
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.VariationsResult");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.Variation");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.ComplexVariation");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.NoVariation");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.JXPathVariation");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.DeepCloningVariation");
            tryToLoadAndAppend(overrideTypes, "org.openl.rules.variation.ArgumentReplacementVariation");
        }
        return overrideTypes;
    }

    private void tryToLoadAndAppend(Set<String> overrideTypes, String className) {
        tryToLoadClass(className);
        overrideTypes.add(className);
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
