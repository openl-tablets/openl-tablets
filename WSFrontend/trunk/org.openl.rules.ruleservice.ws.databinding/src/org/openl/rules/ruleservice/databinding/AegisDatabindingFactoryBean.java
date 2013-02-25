package org.openl.rules.ruleservice.databinding;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.type.TypeCreationOptions;
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
import org.openl.rules.variation.ArgumentReplacementVariation;
import org.openl.rules.variation.ComplexVariation;
import org.openl.rules.variation.DeepCloningVariaion;
import org.openl.rules.variation.JXPathVariation;
import org.openl.rules.variation.NoVariation;
import org.openl.rules.variation.Variation;

public class AegisDatabindingFactoryBean {

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
        typeMapping.register(new RuntimeContextBeanType());
        if (supportVariations) {
            typeMapping.register(new VariationsResultType());
            typeMapping.register(new JXPathVariationType());
            typeMapping.register(new ArgumentReplacementVariationType());
            typeMapping.register(new DeepCloningVariationType());
            typeMapping.register(new ComplexVariationType());
        }
        typeMapping.register(new IntRangeBeanType());
        typeMapping.register(new DoubleRangeBeanType());
        typeMapping.register(ShortValue.class, XMLSchemaQNames.XSD_SHORT, new ShortValueType());
        typeMapping.register(LongValue.class, XMLSchemaQNames.XSD_LONG, new LongValueType());
        typeMapping.register(IntValue.class, XMLSchemaQNames.XSD_INT, new IntValueType());
        typeMapping.register(FloatValue.class, XMLSchemaQNames.XSD_FLOAT, new FloatValueType());
        typeMapping.register(DoubleValue.class, XMLSchemaQNames.XSD_DOUBLE, new DoubleValueType());
        typeMapping.register(ByteValue.class, XMLSchemaQNames.XSD_BYTE, new ByteValueType());
        typeMapping.register(BigIntegerValue.class, XMLSchemaQNames.XSD_INTEGER, new BigIntegerValueType());
        typeMapping.register(BigDecimalValue.class, XMLSchemaQNames.XSD_DECIMAL, new BigDecimalValueType());
        
        return aegisDatabinding;
    }

    protected Set<String> getOverrideTypesWithDefaultOpenLTypes() {
        Set<String> overrideTypes = new HashSet<String>();
        if (getOverrideTypes() != null) {
            overrideTypes.addAll(getOverrideTypes());
        }
        if (supportVariations) {
            overrideTypes.add(Variation.class.getCanonicalName());
            overrideTypes.add(ComplexVariation.class.getName());
            overrideTypes.add(NoVariation.class.getName());
            overrideTypes.add(JXPathVariation.class.getName());
            overrideTypes.add(DeepCloningVariaion.class.getName());
            overrideTypes.add(ArgumentReplacementVariation.class.getName());
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
