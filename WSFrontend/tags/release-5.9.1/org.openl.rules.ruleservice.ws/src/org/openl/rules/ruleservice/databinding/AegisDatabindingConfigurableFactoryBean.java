package org.openl.rules.ruleservice.databinding;

import java.util.Collection;
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
import org.openl.rules.ruleservice.context.BigDecimalValueType;
import org.openl.rules.ruleservice.context.BigIntegerValueType;
import org.openl.rules.ruleservice.context.ByteValueType;
import org.openl.rules.ruleservice.context.DoubleRangeBeanType;
import org.openl.rules.ruleservice.context.DoubleValueType;
import org.openl.rules.ruleservice.context.FloatValueType;
import org.openl.rules.ruleservice.context.IntRangeBeanType;
import org.openl.rules.ruleservice.context.IntValueType;
import org.openl.rules.ruleservice.context.LongValueType;
import org.openl.rules.ruleservice.context.RuntimeContextBeanType;
import org.openl.rules.ruleservice.context.ShortValueType;
import org.springframework.beans.factory.FactoryBean;

public class AegisDatabindingConfigurableFactoryBean implements FactoryBean<AegisDatabinding> {

    private Boolean writeXsiTypes;
    private Set<String> overrideTypes;
    private TypeCreationOptions configuration;
    private Boolean mtomUseXmime;
    private Boolean mtomEnabled;
    private Integer mtomThreshold;
    private Bus bus;
    private Collection<DOMSource> schemas;
    private Map<String, String> namespaceMap;

    @Override
    public AegisDatabinding getObject() throws Exception {
        AegisDatabinding aegisDatabinding = new AegisDatabinding();
        if (getOverrideTypes() != null) {
            aegisDatabinding.setOverrideTypes(getOverrideTypes());
        }

        if (getConfiguration() != null) {
            aegisDatabinding.setConfiguration(getConfiguration());
        }

        if (getWriteXsiTypes() != null) {
            aegisDatabinding.getAegisContext().setWriteXsiTypes(getWriteXsiTypes().booleanValue());
        }

        if (getMtomUseXmime() != null) {
            aegisDatabinding.setMtomUseXmime(getMtomUseXmime().booleanValue());
        }

        if (getBus() != null) {
            aegisDatabinding.setBus(getBus());
        }

        if (getMtomEnabled() != null) {
            aegisDatabinding.setMtomEnabled(getMtomEnabled().booleanValue());
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

        fillDefaultOpenLTypesMappings(aegisDatabinding);
        return aegisDatabinding;
    }

    protected void fillDefaultOpenLTypesMappings(AegisDatabinding aegisDatabinding) {
        TypeMapping typeMapping = aegisDatabinding.getAegisContext().getTypeMapping();
        typeMapping.register(new RuntimeContextBeanType());
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
    }

    @Override
    public Class<AegisDatabinding> getObjectType() {
        return AegisDatabinding.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
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

}