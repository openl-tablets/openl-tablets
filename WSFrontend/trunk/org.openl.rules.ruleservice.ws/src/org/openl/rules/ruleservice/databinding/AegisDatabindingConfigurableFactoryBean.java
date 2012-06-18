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
import org.openl.rules.project.instantiation.variation.ArgumentReplacementVariation;
import org.openl.rules.project.instantiation.variation.ComplexVariation;
import org.openl.rules.project.instantiation.variation.DeepCloninigVariaion;
import org.openl.rules.project.instantiation.variation.JXPathVariation;
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
        aegisDatabinding.setOverrideTypes(getOverrideTypesWithDefaultOpenLTypes());

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

    protected Set<String> getOverrideTypesWithDefaultOpenLTypes() {
        Set<String> overrideTypes = new HashSet<String>();
        if(getOverrideTypes() != null){
            overrideTypes.addAll(getOverrideTypes());
        }
        overrideTypes.add(JXPathVariation.class.getName());
        overrideTypes.add(DeepCloninigVariaion.class.getName());
        overrideTypes.add(ComplexVariation.class.getName());
        overrideTypes.add(ArgumentReplacementVariation.class.getName());
        return overrideTypes;
    }

    protected void fillDefaultOpenLTypesMappings(AegisDatabinding aegisDatabinding) {
        TypeMapping typeMapping = aegisDatabinding.getAegisContext().getTypeMapping();
        OpenLTypeMapping openLTypeMapping= new OpenLTypeMapping(typeMapping);
        aegisDatabinding.getAegisContext().setTypeMapping(openLTypeMapping);
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