package org.openl.rules.table.properties;

import java.util.Map;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;

public interface ITableProperties {
        
    Map<String, Object> getPropertiesAll();
    
    /**
     * Gets the <code>{@link Map}</code> of properties with name as key and value as value, this map
     * excludes properties that were set by default. So it will contain all properties in source table including 
     * system ones.
     * 
     * @return Map of properties excluding properties set by default.
     */
    Map<String, Object> getPropertiesDefinedInTable();
    
    /**
     * Gets the <code>{@link Map}</code> of properties with name as key and value as value, this map
     * excludes properties that were set by default and also properties that were set by system. 
     * So it will contain just part of properties in source table excluding system ones.
     * 
     * @return Map of properties excluding properties set by default.
     */
    Map<String, Object> getPropertiesDefinedInTableIgnoreSystem();
    
    Map<String, Object> getPropertiesAppliedForCategory();
    
    Map<String, Object> getPropertiesAppliedForModule();
    
    Map<String, Object> getPropertiesAppliedByDefault();
    
    Object getPropertyValue(String key);
    
    /**
     * Returns the value of the property as <code>String</code>. 
     * If the current property value is of <code>Date</code> type,
     * gets the format of date from {@link DefaultPropertyDefinitions}.
     * @param key Name of the property.
     * @return Value formatted to string. <code>Null</code> when there is
     * no property with such name.
     */
    String getPropertyValueAsString(String key);
    
    /**
     * Gets the logical table of the properties defined in table.
     */
    ILogicalTable getPropertiesSection();
	
	// <<< INSERT >>>
	java.lang.String getName();
	void setName(java.lang.String name);	
	java.lang.String getCategory();
	void setCategory(java.lang.String category);	
	java.lang.String getDescription();
	void setDescription(java.lang.String description);	
	java.lang.String getTags();
	void setTags(java.lang.String tags);	
	java.util.Date getEffectiveDate();
	void setEffectiveDate(java.util.Date effectiveDate);	
	java.util.Date getExpirationDate();
	void setExpirationDate(java.util.Date expirationDate);	
	java.lang.String getCreatedBy();
	void setCreatedBy(java.lang.String createdBy);	
	java.util.Date getCreatedOn();
	void setCreatedOn(java.util.Date createdOn);	
	java.lang.String getModifiedBy();
	void setModifiedBy(java.lang.String modifiedBy);	
	java.util.Date getModifyOn();
	void setModifyOn(java.util.Date modifyOn);	
	java.lang.String getBuildPhase();
	void setBuildPhase(java.lang.String buildPhase);	
	java.lang.String getValidateDT();
	void setValidateDT(java.lang.String validateDT);	
	java.lang.String getLob();
	void setLob(java.lang.String lob);	
	java.lang.String getUsregion();
	void setUsregion(java.lang.String usregion);	
	java.lang.String getCountry();
	void setCountry(java.lang.String country);	
	java.lang.String getCurrency();
	void setCurrency(java.lang.String currency);	
	java.lang.String getLang();
	void setLang(java.lang.String lang);	
	java.lang.String getState();
	void setState(java.lang.String state);	
	java.lang.String getRegion();
	void setRegion(java.lang.String region);	
	java.lang.String getVersion();
	void setVersion(java.lang.String version);	
	java.lang.Boolean getActive();
	void setActive(java.lang.Boolean active);	
	java.lang.Boolean getFailOnMiss();
	void setFailOnMiss(java.lang.Boolean failOnMiss);	
	java.lang.Boolean getReturnOnMiss();
	void setReturnOnMiss(java.lang.Boolean returnOnMiss);	
	java.lang.String getScope();
	void setScope(java.lang.String scope);	
	// <<< END INSERT >>>
	
	
    void setPropertiesAppliedForCategory(Map<String, Object> categoryProperties);
    
    void setPropertiesAppliedForModule(Map<String, Object> moduleProperties);    
    
    void setPropertiesAppliedByDefault(Map<String, Object> defaultProperties);    
	
}
