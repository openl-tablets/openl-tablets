package org.openl.rules.table.properties;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.EnumUtils;
import org.openl.util.StringUtils;

public class TableProperties extends DynamicObject implements ITableProperties {

    private String currentTableType;
    /**
     * Table section that contains properties in appropriate table in data
     * source.
     */
    private ILogicalTable propertySection;

    private TableSyntaxNode modulePropertiesTableSyntaxNode;

    private TableSyntaxNode categoryPropertiesTableSyntaxNode;

    private Map<String, Object> categoryProperties = Collections.emptyMap();

    private Map<String, Object> externalModuleProperties = Collections.emptyMap();

    private Map<String, Object> moduleProperties = Collections.emptyMap();;

    private Map<String, Object> defaultProperties = Collections.emptyMap();;

    /**
     * The result <code>{@link Map}</code> will contain all pairs from
     * downLevelProperties and pairs from upLevelProperties that are not defined
     * in downLevelProperties. Ignore properties from upper level that can`t be
     * defined for current table type.
     *
     * @param downLevelProperties properties that are on the down level.
     * @param upLevelProperties properties that are on the up level.
     * @return
     */
    private Map<String, Object> mergeLevelProperties(Map<String, Object> downLevelProperties,
            Map<String, Object> upLevelProperties) {
        Map<String, Object> resultProperties = downLevelProperties;
        for (Entry<String, Object> upLevelProperty : upLevelProperties.entrySet()) {
            String upLevelPropertyName = upLevelProperty.getKey();
            Object upLevelPropertyValue = upLevelProperty.getValue();

            if (PropertiesChecker.isPropertySuitableForTableType(upLevelPropertyName, currentTableType)) {
                if (!downLevelProperties.containsKey(upLevelPropertyName)) {
                    resultProperties.put(upLevelPropertyName, upLevelPropertyValue);
                }
            }
        }
        return resultProperties;
    }

    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(getClass());
    }

    @Override
    public void setType(IOpenClass type) {
        throw new UnsupportedOperationException();
    }

    // <<< INSERT >>>
    @Override
    public java.lang.String getName() {
        return (java.lang.String) getPropertyValue("name");
    }

    @Override
    public void setName(java.lang.String name) {
        setFieldValue("name", name);
        reset();
    }
    @Override
    public java.lang.String getCategory() {
        return (java.lang.String) getPropertyValue("category");
    }

    @Override
    public void setCategory(java.lang.String category) {
        setFieldValue("category", category);
        reset();
    }
    @Override
    public java.lang.String getCreatedBy() {
        return (java.lang.String) getPropertyValue("createdBy");
    }

    @Override
    public void setCreatedBy(java.lang.String createdBy) {
        setFieldValue("createdBy", createdBy);
        reset();
    }
    @Override
    public java.util.Date getCreatedOn() {
        return (java.util.Date) getPropertyValue("createdOn");
    }

    @Override
    public void setCreatedOn(java.util.Date createdOn) {
        setFieldValue("createdOn", createdOn);
        reset();
    }
    @Override
    public java.lang.String getModifiedBy() {
        return (java.lang.String) getPropertyValue("modifiedBy");
    }

    @Override
    public void setModifiedBy(java.lang.String modifiedBy) {
        setFieldValue("modifiedBy", modifiedBy);
        reset();
    }
    @Override
    public java.util.Date getModifiedOn() {
        return (java.util.Date) getPropertyValue("modifiedOn");
    }

    @Override
    public void setModifiedOn(java.util.Date modifiedOn) {
        setFieldValue("modifiedOn", modifiedOn);
        reset();
    }
    @Override
    public java.lang.String getDescription() {
        return (java.lang.String) getPropertyValue("description");
    }

    @Override
    public void setDescription(java.lang.String description) {
        setFieldValue("description", description);
        reset();
    }
    @Override
    public java.lang.String[] getTags() {
        return (java.lang.String[]) getPropertyValue("tags");
    }

    @Override
    public void setTags(java.lang.String... tags) {
        setFieldValue("tags", tags);
        reset();
    }
    @Override
    public java.util.Date getEffectiveDate() {
        return (java.util.Date) getPropertyValue("effectiveDate");
    }

    @Override
    public void setEffectiveDate(java.util.Date effectiveDate) {
        setFieldValue("effectiveDate", effectiveDate);
        reset();
    }
    @Override
    public java.util.Date getExpirationDate() {
        return (java.util.Date) getPropertyValue("expirationDate");
    }

    @Override
    public void setExpirationDate(java.util.Date expirationDate) {
        setFieldValue("expirationDate", expirationDate);
        reset();
    }
    @Override
    public java.util.Date getStartRequestDate() {
        return (java.util.Date) getPropertyValue("startRequestDate");
    }

    @Override
    public void setStartRequestDate(java.util.Date startRequestDate) {
        setFieldValue("startRequestDate", startRequestDate);
        reset();
    }
    @Override
    public java.util.Date getEndRequestDate() {
        return (java.util.Date) getPropertyValue("endRequestDate");
    }

    @Override
    public void setEndRequestDate(java.util.Date endRequestDate) {
        setFieldValue("endRequestDate", endRequestDate);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.CaRegionsEnum[] getCaRegions() {
        return (org.openl.rules.enumeration.CaRegionsEnum[]) getPropertyValue("caRegions");
    }

    @Override
    public void setCaRegions(org.openl.rules.enumeration.CaRegionsEnum... caRegions) {
        setFieldValue("caRegions", caRegions);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.CaProvincesEnum[] getCaProvinces() {
        return (org.openl.rules.enumeration.CaProvincesEnum[]) getPropertyValue("caProvinces");
    }

    @Override
    public void setCaProvinces(org.openl.rules.enumeration.CaProvincesEnum... caProvinces) {
        setFieldValue("caProvinces", caProvinces);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.CountriesEnum[] getCountry() {
        return (org.openl.rules.enumeration.CountriesEnum[]) getPropertyValue("country");
    }

    @Override
    public void setCountry(org.openl.rules.enumeration.CountriesEnum... country) {
        setFieldValue("country", country);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.RegionsEnum[] getRegion() {
        return (org.openl.rules.enumeration.RegionsEnum[]) getPropertyValue("region");
    }

    @Override
    public void setRegion(org.openl.rules.enumeration.RegionsEnum... region) {
        setFieldValue("region", region);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.CurrenciesEnum[] getCurrency() {
        return (org.openl.rules.enumeration.CurrenciesEnum[]) getPropertyValue("currency");
    }

    @Override
    public void setCurrency(org.openl.rules.enumeration.CurrenciesEnum... currency) {
        setFieldValue("currency", currency);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.LanguagesEnum[] getLang() {
        return (org.openl.rules.enumeration.LanguagesEnum[]) getPropertyValue("lang");
    }

    @Override
    public void setLang(org.openl.rules.enumeration.LanguagesEnum... lang) {
        setFieldValue("lang", lang);
        reset();
    }
    @Override
    public java.lang.String[] getLob() {
        return (java.lang.String[]) getPropertyValue("lob");
    }

    @Override
    public void setLob(java.lang.String... lob) {
        setFieldValue("lob", lob);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.OriginsEnum getOrigin() {
        return (org.openl.rules.enumeration.OriginsEnum) getPropertyValue("origin");
    }

    @Override
    public void setOrigin(org.openl.rules.enumeration.OriginsEnum origin) {
        setFieldValue("origin", origin);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.UsRegionsEnum[] getUsregion() {
        return (org.openl.rules.enumeration.UsRegionsEnum[]) getPropertyValue("usregion");
    }

    @Override
    public void setUsregion(org.openl.rules.enumeration.UsRegionsEnum... usregion) {
        setFieldValue("usregion", usregion);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.UsStatesEnum[] getState() {
        return (org.openl.rules.enumeration.UsStatesEnum[]) getPropertyValue("state");
    }

    @Override
    public void setState(org.openl.rules.enumeration.UsStatesEnum... state) {
        setFieldValue("state", state);
        reset();
    }
    @Override
    public java.lang.String getVersion() {
        return (java.lang.String) getPropertyValue("version");
    }

    @Override
    public void setVersion(java.lang.String version) {
        setFieldValue("version", version);
        reset();
    }
    @Override
    public java.lang.Boolean getActive() {
        return (java.lang.Boolean) getPropertyValue("active");
    }

    @Override
    public void setActive(java.lang.Boolean active) {
        setFieldValue("active", active);
        reset();
    }
    @Override
    public java.lang.String getId() {
        return (java.lang.String) getPropertyValue("id");
    }

    @Override
    public void setId(java.lang.String id) {
        setFieldValue("id", id);
        reset();
    }
    @Override
    public java.lang.String getBuildPhase() {
        return (java.lang.String) getPropertyValue("buildPhase");
    }

    @Override
    public void setBuildPhase(java.lang.String buildPhase) {
        setFieldValue("buildPhase", buildPhase);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.ValidateDTEnum getValidateDT() {
        return (org.openl.rules.enumeration.ValidateDTEnum) getPropertyValue("validateDT");
    }

    @Override
    public void setValidateDT(org.openl.rules.enumeration.ValidateDTEnum validateDT) {
        setFieldValue("validateDT", validateDT);
        reset();
    }
    @Override
    public java.lang.Boolean getFailOnMiss() {
        return (java.lang.Boolean) getPropertyValue("failOnMiss");
    }

    @Override
    public void setFailOnMiss(java.lang.Boolean failOnMiss) {
        setFieldValue("failOnMiss", failOnMiss);
        reset();
    }
    @Override
    public java.lang.String getScope() {
        return (java.lang.String) getPropertyValue("scope");
    }

    @Override
    public void setScope(java.lang.String scope) {
        setFieldValue("scope", scope);
        reset();
    }
    @Override
    public java.lang.String getDatatypePackage() {
        return (java.lang.String) getPropertyValue("datatypePackage");
    }

    @Override
    public void setDatatypePackage(java.lang.String datatypePackage) {
        setFieldValue("datatypePackage", datatypePackage);
        reset();
    }
    @Override
    public java.lang.Boolean getCacheable() {
        return (java.lang.Boolean) getPropertyValue("cacheable");
    }

    @Override
    public void setCacheable(java.lang.Boolean cacheable) {
        setFieldValue("cacheable", cacheable);
        reset();
    }
    @Override
    public org.openl.rules.enumeration.RecalculateEnum getRecalculate() {
        return (org.openl.rules.enumeration.RecalculateEnum) getPropertyValue("recalculate");
    }

    @Override
    public void setRecalculate(org.openl.rules.enumeration.RecalculateEnum recalculate) {
        setFieldValue("recalculate", recalculate);
        reset();
    }
    @Override
    public java.lang.String getPrecision() {
        return (java.lang.String) getPropertyValue("precision");
    }

    @Override
    public void setPrecision(java.lang.String precision) {
        setFieldValue("precision", precision);
        reset();
    }
    @Override
    public java.lang.Boolean getAutoType() {
        return (java.lang.Boolean) getPropertyValue("autoType");
    }

    @Override
    public void setAutoType(java.lang.Boolean autoType) {
        setFieldValue("autoType", autoType);
        reset();
    }
    @Override
    public java.lang.Boolean getCalculateAllCells() {
        return (java.lang.Boolean) getPropertyValue("calculateAllCells");
    }

    @Override
    public void setCalculateAllCells(java.lang.Boolean calculateAllCells) {
        setFieldValue("calculateAllCells", calculateAllCells);
        reset();
    }
    @Override
    public java.lang.Boolean getParallel() {
        return (java.lang.Boolean) getPropertyValue("parallel");
    }

    @Override
    public void setParallel(java.lang.Boolean parallel) {
        setFieldValue("parallel", parallel);
        reset();
    }
    @Override
    public java.lang.String getNature() {
        return (java.lang.String) getPropertyValue("nature");
    }

    @Override
    public void setNature(java.lang.String nature) {
        setFieldValue("nature", nature);
        reset();
    }
// <<< END INSERT >>>

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getPropertyValue(String key) {
        return getAllProperties().get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyValueAsString(String key) {
        String result = null;
        Object propValue = getPropertyValue(key);
        if (propValue != null) {
            if (propValue instanceof Date) {
                String format = TablePropertyDefinitionUtils.getPropertyByName(key).getFormat();
                if (format != null) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                    result = dateFormat.format((Date) propValue);
                }
            } else if (EnumUtils.isEnum(propValue)) {
                result = ((Enum<?>) propValue).name();
            } else if (EnumUtils.isEnumArray(propValue)) {

                Object[] enums = (Object[]) propValue;

                if (!ArrayTool.isEmpty(enums)) {

                    String[] names = EnumUtils.getNames(enums);
                    result = StringUtils.join(names, ",");
                } else {
                    result = "";
                }
            } else if (propValue.getClass().isArray()) {
                Object[] array = (Object[]) propValue;
                if (!ArrayTool.isEmpty(array)) {
                    result = StringUtils.join(array, ",");
                } else {
                    result = "";
                }
            } else {
                result = propValue.toString();
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InheritanceLevel getPropertyLevelDefinedOn(String propertyName) {
        InheritanceLevel result = null;
        if (getTableProperties().containsKey(propertyName)) {
            result = InheritanceLevel.TABLE;
        } else if (getCategoryProperties().containsKey(propertyName)) {
            result = InheritanceLevel.CATEGORY;
        } else if (getModuleProperties().containsKey(propertyName)) {
            result = InheritanceLevel.MODULE;
        } else if (getExternalProperties().containsKey(propertyName)) {
            result = InheritanceLevel.EXTERNAL;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertyAppliedByDefault(String propertyName) {
        boolean result = false;
        if (getPropertyLevelDefinedOn(propertyName) == null && defaultProperties.containsKey(propertyName)) {
            result = true;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ILogicalTable getPropertiesSection() {
        return propertySection;
    }

    @Override
    public void setPropertiesSection(ILogicalTable propertySection) {
        this.propertySection = propertySection;
    }
    
    @Override
    public TableSyntaxNode getModulePropertiesTableSyntaxNode() {
        return modulePropertiesTableSyntaxNode;
    }

    @Override
    public void setModulePropertiesTableSyntaxNode(TableSyntaxNode modulePropertiesTableSyntaxNode) {
        this.modulePropertiesTableSyntaxNode = modulePropertiesTableSyntaxNode;
    }

    @Override
    public TableSyntaxNode getCategoryPropertiesTableSyntaxNode() {
        return categoryPropertiesTableSyntaxNode;
    }

    @Override
    public void setCategoryPropertiesTableSyntaxNode(TableSyntaxNode categoryPropertiesTableSyntaxNode) {
        this.categoryPropertiesTableSyntaxNode = categoryPropertiesTableSyntaxNode;
    }

    @Override
    public TableSyntaxNode getInheritedPropertiesTableSyntaxNode(InheritanceLevel inheritanceLevel) {
        if (InheritanceLevel.MODULE.equals(inheritanceLevel)) {
            return modulePropertiesTableSyntaxNode;
        } else if (InheritanceLevel.CATEGORY.equals(inheritanceLevel)) {
            return categoryPropertiesTableSyntaxNode;
        }
        return null;
    }

    private Map<String, Object> allProperties = null;

    private Map<String, Object> allDimensionalProperties = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAllProperties() {
        if (allProperties != null) {
            return allProperties;
        }
        Map<String, Object> tableAndCategoryProp = mergeLevelProperties(super.getFieldValues(), categoryProperties);
        Map<String, Object> tableAndCategoryAndModuleProp = mergeLevelProperties(tableAndCategoryProp,
            moduleProperties);
        Map<String, Object> tableAndCategoryAndModuleAndExteranlProp = mergeLevelProperties(
            tableAndCategoryAndModuleProp,
            externalModuleProperties);
        Map<String, Object> allTableProperties = mergeLevelProperties(tableAndCategoryAndModuleAndExteranlProp,
            defaultProperties);
        allProperties = Collections.unmodifiableMap(allTableProperties);
        return allProperties;
    }

    @Override
    public void setFieldValue(String name, Object value) {
        super.setFieldValue(name, toPropertyValue(value));
    }

    private Object toPropertyValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().isArray()) {
            Object[] value1 = ((Object[]) value).clone();
            Arrays.sort(value1);
            return value1;
        }
        if (value instanceof Date) {
            return ((Date) value).clone();
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getTableProperties() {
        return super.getFieldValues();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAllDimensionalProperties() {
        if (allDimensionalProperties == null) {
            Map<String, Object> tmp = new HashMap<>();
            Map<String, Object> props = getAllProperties();
            for (Map.Entry<String, Object> property : props.entrySet()) {
                String propName = property.getKey();
                TablePropertyDefinition propertyDefinition = TablePropertyDefinitionUtils.getPropertyByName(propName);
                if (propertyDefinition.isDimensional()) {
                    tmp.put(propName, property.getValue());
                }
            }
            allDimensionalProperties = Collections.unmodifiableMap(tmp);
        }
        return allDimensionalProperties;
    }

    private void reset() {
        allProperties = null;
        allDimensionalProperties = null;
    }

    @Override
    public void setCategoryProperties(Map<String, Object> categoryProperties) {
        if (categoryProperties == null) {
            this.categoryProperties = Collections.emptyMap();
        } else {
            this.categoryProperties = extractPropertiesMap(categoryProperties);
        }
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getCategoryProperties() {
        return categoryProperties;
    }

    @Override
    public void setModuleProperties(Map<String, Object> moduleProperties) {
        if (moduleProperties == null) {
            this.moduleProperties = Collections.emptyMap();
        } else {
            this.moduleProperties = extractPropertiesMap(moduleProperties);
        }
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getModuleProperties() {
        return moduleProperties;
    }

    @Override
    public void setDefaultProperties(Map<String, Object> defaultProperties) {
        if (defaultProperties == null) {
            this.defaultProperties = Collections.emptyMap();
        } else {
            this.defaultProperties = Collections.unmodifiableMap(defaultProperties);
        }
        reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getDefaultProperties() {
        return defaultProperties;
    }

    @Override
    public void setCurrentTableType(String currentTableType) {
        this.currentTableType = currentTableType;
    }

    @Override
    public String getCurrentTableType() {
        return currentTableType;
    }

    @Override
    public Map<String, Object> getExternalProperties() {
        return externalModuleProperties;
    }

    @Override
    public void setExternalProperties(Map<String, Object> externalProperties) {
        if (externalProperties == null) {
            this.externalModuleProperties = Collections.emptyMap();
        } else {
            this.externalModuleProperties = extractPropertiesMap(externalProperties);
        }
        reset();
    }

    private Map<String, Object> extractPropertiesMap(Map<String, Object> externalProperties) {
        Map<String, Object> tmp = new HashMap<>();
        for (Entry<String, Object> entry : externalProperties.entrySet()) {
            tmp.put(entry.getKey(), toPropertyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(tmp);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPropertiesEmpty() {
        Set<String> keys = getAllProperties().keySet();
        for (String key : keys) {
            if (getAllProperties().get(key) != null) {
                return false;
            }
        }
        return true;
    }

}
