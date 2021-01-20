package org.openl.rules.ruleservice.databinding;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeMapping;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ArgumentReplacementVariationType;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.ComplexVariationType;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.DeepCloningVariationType;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.JXPathVariationType;
import org.openl.rules.ruleservice.databinding.aegis.org.openl.rules.variation.VariationsResultType;
import org.openl.rules.ruleservice.databinding.annotation.JacksonBindingConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAegisDatabindingFactoryBean {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractAegisDatabindingFactoryBean.class);

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
    private boolean supportVariations;

    public AegisDatabinding createAegisDatabinding() {
        AegisContext aegisContext = new OpenLAegisContext();
        AegisDatabinding aegisDatabinding = new AegisDatabinding(aegisContext);
        if (getConfiguration() != null) {
            aegisDatabinding.setConfiguration(configuration);
            aegisContext.setTypeCreationOptions(configuration);
        } else {
            TypeCreationOptions typeCreationOptions = new TypeCreationOptions();
            typeCreationOptions.setDefaultNillable(false);
            typeCreationOptions.setDefaultMinOccurs(0);
            aegisDatabinding.setConfiguration(typeCreationOptions);
            aegisContext.setTypeCreationOptions(typeCreationOptions);
        }

        if (getMtomUseXmime() != null) {
            aegisDatabinding.setMtomUseXmime(getMtomUseXmime());
            aegisContext.setMtomUseXmime(getMtomUseXmime());
        }

        if (getMtomEnabled() != null) {
            aegisDatabinding.setMtomEnabled(getMtomEnabled());
            aegisContext.setMtomEnabled(getMtomEnabled());
        }

        Set<String> rootClassNames = getPreparedOverrideTypes();
        aegisDatabinding.setOverrideTypes(rootClassNames);
        aegisContext.setRootClassNames(rootClassNames);
        aegisContext.initialize();
        if (getBus() != null) {
            aegisDatabinding.setBus(getBus());
        }

        if (getMtomThreshold() != null) {
            aegisDatabinding.setMtomThreshold(getMtomThreshold());
        }

        if (getNamespaceMap() != null) {
            aegisDatabinding.setNamespaceMap(getNamespaceMap());
        }

        if (getSchemas() != null) {
            aegisDatabinding.setSchemas(getSchemas());
        }

        if (getWriteXsiTypes() != null) {
            aegisDatabinding.getAegisContext().setWriteXsiTypes(getWriteXsiTypes());
        }

        if (getReadXsiTypes() != null) {
            aegisDatabinding.getAegisContext().setReadXsiTypes(getReadXsiTypes());
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
        loadAegisTypeClassAndRegister(VariationsResultType.class, typeMapping);
        loadAegisTypeClassAndRegister(JXPathVariationType.class, typeMapping);
        loadAegisTypeClassAndRegister(ArgumentReplacementVariationType.class, typeMapping);
        loadAegisTypeClassAndRegister(DeepCloningVariationType.class, typeMapping);
        loadAegisTypeClassAndRegister(ComplexVariationType.class, typeMapping);
    }

    protected abstract void registerOpenLTypes(TypeMapping typeMapping);

    protected abstract void registerCustomJavaTypes(TypeMapping typeMapping);

    private static AegisType instantiateAegisType(Class<?> clazz) throws NoSuchMethodException,
                                                                         InstantiationException,
                                                                         IllegalAccessException,
                                                                         InvocationTargetException {
        Constructor<?> constructor = clazz.getConstructor();
        return (AegisType) constructor.newInstance();
    }

    protected void loadAegisTypeClassAndRegister(Class<?> aegisTypeClass, TypeMapping typeMapping) {
        try {
            AegisType aegisType = instantiateAegisType(aegisTypeClass);
            typeMapping.register(aegisType);
        } catch (Exception e) {
            LOG.warn("Aegis type '{}' registration failed.", aegisTypeClass.getName(), e);
        }
    }

    protected void loadAegisTypeClassAndRegister(String typeClassName,
            Class<?> aegisTypeClass,
            QName qName,
            TypeMapping typeMapping) {
        try {
            Class<?> typeClazz = Thread.currentThread().getContextClassLoader().loadClass(typeClassName);
            AegisType aegisType = instantiateAegisType(aegisTypeClass);
            typeMapping.register(typeClazz, qName, aegisType);
        } catch (Exception e) {
            LOG.warn("Type '{}' registration failed.", typeClassName, e);
        }
    }

    protected void loadAegisTypeClassAndRegister(Class<?> typeClazz,
            Class<?> aegisTypeClass,
            QName qName,
            TypeMapping typeMapping) {
        try {
            AegisType aegisType = instantiateAegisType(aegisTypeClass);
            typeMapping.register(typeClazz, qName, aegisType);
        } catch (Exception e) {
            LOG.warn("Type '{}' registration failed.", typeClazz.getName(), e);
        }
    }

    private static Class<?> tryToLoadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            LOG.warn("Class '{}' is not found.", className, e);
        }
        return null;
    }

    private void addClassesWithXmlSeeAlso(Set<String> overrideTypes, Class<?> clazz) {
        if (!JacksonBindingConfigurationUtils.isConfiguration(clazz)) {
            XmlSeeAlso xmlSeeAlso = clazz.getAnnotation(XmlSeeAlso.class);
            if (xmlSeeAlso != null) {
                for (Class<?> cls : xmlSeeAlso.value()) {
                    overrideTypes.add(cls.getName());
                    addClassesWithXmlSeeAlso(overrideTypes, cls);
                }
            }
        }
    }

    private void addClassesWithXmlSeeAlso(Set<String> overrideTypes) {
        Set<String> tmp = new HashSet<>(overrideTypes);
        for (String className : tmp) {
            Class<?> cls = tryToLoadClass(className);
            if (cls != null) {
                addClassesWithXmlSeeAlso(overrideTypes, cls);
            }
        }
    }

    protected Set<String> getPreparedOverrideTypes() {
        Set<String> preparedOverrideTypes = new HashSet<>();
        if (getOverrideTypes() != null) {
            for (String className : getOverrideTypes()) {
                Class<?> clazz = tryToLoadClass(className);
                if (!JacksonBindingConfigurationUtils.isConfiguration(clazz)) {
                    preparedOverrideTypes.add(className);
                }
            }
        }

        addClassesWithXmlSeeAlso(preparedOverrideTypes);

        preparedOverrideTypes.add(SpreadsheetResult.class.getName());

        if (supportVariations) {
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.VariationsResult");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.Variation");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.ComplexVariation");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.NoVariation");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.JXPathVariation");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.DeepCloningVariation");
            tryToLoadAndAdd(preparedOverrideTypes, "org.openl.rules.variation.ArgumentReplacementVariation");
        }
        return preparedOverrideTypes;
    }

    private void tryToLoadAndAdd(Set<String> overrideTypes, String className) {
        Class<?> cls = tryToLoadClass(className);
        if (cls != null) {
            overrideTypes.add(className);
        }
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
