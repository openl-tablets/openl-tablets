package org.openl.rules.table.properties;

import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;

public interface ITableProperties {

    /**
     * <code>{@link Map}</code> of properties that includes all properties for current table. It includes: - all
     * properties physically defined in table with system ones; - inherited properties from category and module scopes;
     * - properties set by default;
     *
     * @return <code>{@link Map}</code> of all properties relevant to current table.
     */
    Map<String, Object> getAllProperties();

    /**
     * Gets the <code>{@link Map}</code> of properties with name as key and value as value, this map contains all
     * properties defined in source table. No inherited and no default properties.
     *
     * @return <code>{@link Map}</code> of properties defined in table.
     */
    Map<String, Object> getTableProperties();

    /**
     * Gets the <code>{@link Map}</code> of properties with name as key and value as value, this map contains all
     * dimensional properties defined for table. To find out which property is dimensional see property definitions
     * {@link TablePropertyDefinitionUtils#getSystemProperties()}.
     *
     * @return <code>{@link Map}</code> of properties defined in table excluding system properties.
     */
    Map<String, Object> getAllDimensionalProperties();

    /**
     * <code>{@link Map}</code> of properties applied to the category this table belongs to.
     *
     * @return <code>{@link Map}</code> of properties applied to the category this table belongs to.
     */
    Map<String, Object> getCategoryProperties();

    /**
     * <code>{@link Map}</code> of properties applied to the module this table belongs to.
     *
     * @return <code>{@link Map}</code> of properties applied to the module this table belongs to.
     */
    Map<String, Object> getModuleProperties();

    Map<String, Object> getExternalProperties();

    /**
     * <code>{@link Map}</code> of properties that must be set by default. Default properties are set to the table when
     * there is no such property defined on TABLE, CATEGORY and MODULE levels. To find out which property is default see
     * property definitions {@link TablePropertyDefinitionUtils#getSystemProperties()}.
     *
     * @return <code>{@link Map}</code> of properties that must be set by default.
     */
    Map<String, Object> getDefaultProperties();

    /**
     * Gets the value of the property by its name.
     *
     * @param propertyName Property name.
     *
     * @return Property value.
     */
    Object getPropertyValue(String propertyName);

    /**
     * Returns the value of the property as <code>String</code>. If the current property value is of <code>Date</code>
     * type, gets the format of date from {@link DefaultPropertyDefinitions}.
     *
     * @param propertyName Name of the property.
     *
     * @return Value formatted to string. <code>Null</code> when there is no property with such name.
     */
    String getPropertyValueAsString(String propertyName);

    /**
     * Gets the logical table of the properties defined in table.
     */
    ILogicalTable getPropertiesSection();

    void setPropertiesSection(ILogicalTable propertySection);

    TableSyntaxNode getModulePropertiesTableSyntaxNode();

    void setModulePropertiesTableSyntaxNode(TableSyntaxNode modulePropertiesTableSyntaxNode);

    TableSyntaxNode getCategoryPropertiesTableSyntaxNode();

    void setCategoryPropertiesTableSyntaxNode(TableSyntaxNode categoryPropertiesTableSyntaxNode);

    TableSyntaxNode getInheritedPropertiesTableSyntaxNode(InheritanceLevel inheritanceLevel);

    /**
     * Goes through the hierarchy of properties from TABLE to CATEGORY and then to MODULE and returns the level on which
     * property is inherited or defined.
     *
     * @param propertyName Name of the property.
     *
     * @return level on which property is defined. <code>NULL</code> when there is no such property on all these levels.
     *         Or it can be set by default. So check is it applied as default. @see
     *         {@link #isPropertyAppliedByDefault(String)
     *
     */
    InheritanceLevel getPropertyLevelDefinedOn(String propertyName);

    /**
     * Check if the property with given name is applied for current table by default.
     *
     * @param propertyName name of the property.
     *
     * @return <code>TRUE</code> if the property with given name is applied for current table by default.
     */
    boolean isPropertyAppliedByDefault(String propertyName);

    /**
     * Checks that current table doesn't have properties.
     *
     * @return
     */
    boolean isPropertiesEmpty();

    // <<< INSERT >>>
    java.lang.String getName();

    void setName(java.lang.String name);

    java.lang.String getCategory();

    void setCategory(java.lang.String category);

    java.lang.String getCreatedBy();

    void setCreatedBy(java.lang.String createdBy);

    java.util.Date getCreatedOn();

    void setCreatedOn(java.util.Date createdOn);

    java.lang.String getModifiedBy();

    void setModifiedBy(java.lang.String modifiedBy);

    java.util.Date getModifiedOn();

    void setModifiedOn(java.util.Date modifiedOn);

    java.lang.String getDescription();

    void setDescription(java.lang.String description);

    java.lang.String[] getTags();

    void setTags(java.lang.String... tags);

    java.util.Date getEffectiveDate();

    void setEffectiveDate(java.util.Date effectiveDate);

    java.util.Date getExpirationDate();

    void setExpirationDate(java.util.Date expirationDate);

    java.util.Date getStartRequestDate();

    void setStartRequestDate(java.util.Date startRequestDate);

    java.util.Date getEndRequestDate();

    void setEndRequestDate(java.util.Date endRequestDate);

    org.openl.rules.enumeration.CaRegionsEnum[] getCaRegions();

    void setCaRegions(org.openl.rules.enumeration.CaRegionsEnum... caRegions);

    org.openl.rules.enumeration.CaProvincesEnum[] getCaProvinces();

    void setCaProvinces(org.openl.rules.enumeration.CaProvincesEnum... caProvinces);

    org.openl.rules.enumeration.CountriesEnum[] getCountry();

    void setCountry(org.openl.rules.enumeration.CountriesEnum... country);

    org.openl.rules.enumeration.RegionsEnum[] getRegion();

    void setRegion(org.openl.rules.enumeration.RegionsEnum... region);

    org.openl.rules.enumeration.CurrenciesEnum[] getCurrency();

    void setCurrency(org.openl.rules.enumeration.CurrenciesEnum... currency);

    org.openl.rules.enumeration.LanguagesEnum[] getLang();

    void setLang(org.openl.rules.enumeration.LanguagesEnum... lang);

    java.lang.String[] getLob();

    void setLob(java.lang.String... lob);

    org.openl.rules.enumeration.OriginsEnum getOrigin();

    void setOrigin(org.openl.rules.enumeration.OriginsEnum origin);

    org.openl.rules.enumeration.UsRegionsEnum[] getUsregion();

    void setUsregion(org.openl.rules.enumeration.UsRegionsEnum... usregion);

    org.openl.rules.enumeration.UsStatesEnum[] getState();

    void setState(org.openl.rules.enumeration.UsStatesEnum... state);

    java.lang.String getVersion();

    void setVersion(java.lang.String version);

    java.lang.Boolean getActive();

    void setActive(java.lang.Boolean active);

    java.lang.String getId();

    void setId(java.lang.String id);

    java.lang.String getBuildPhase();

    void setBuildPhase(java.lang.String buildPhase);

    org.openl.rules.enumeration.ValidateDTEnum getValidateDT();

    void setValidateDT(org.openl.rules.enumeration.ValidateDTEnum validateDT);

    java.lang.Boolean getFailOnMiss();

    void setFailOnMiss(java.lang.Boolean failOnMiss);

    java.lang.String getScope();

    void setScope(java.lang.String scope);

    java.lang.String getDatatypePackage();

    void setDatatypePackage(java.lang.String datatypePackage);

    java.lang.Boolean getCacheable();

    void setCacheable(java.lang.Boolean cacheable);

    org.openl.rules.enumeration.RecalculateEnum getRecalculate();

    void setRecalculate(org.openl.rules.enumeration.RecalculateEnum recalculate);

    java.lang.String getPrecision();

    void setPrecision(java.lang.String precision);

    java.lang.Boolean getAutoType();

    void setAutoType(java.lang.Boolean autoType);

    java.lang.Boolean getCalculateAllCells();

    void setCalculateAllCells(java.lang.Boolean calculateAllCells);

    java.lang.Boolean getParallel();

    void setParallel(java.lang.Boolean parallel);

    java.lang.String getNature();

    void setNature(java.lang.String nature);
    // <<< END INSERT >>>

    void setCategoryProperties(Map<String, Object> categoryProperties);

    void setModuleProperties(Map<String, Object> moduleProperties);

    void setExternalProperties(Map<String, Object> moduleProperties);

    void setDefaultProperties(Map<String, Object> defaultProperties);

    void setCurrentTableType(String currentTableType);

    String getCurrentTableType();

}
