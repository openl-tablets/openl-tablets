package org.openl.rules.table.properties;

import java.util.HashMap;
import java.util.Map;

public class DefaultTableProperties implements ITableProperties {
	
	private Map<String, Object> internalMap = new HashMap<String, Object>();
	
	public void put(String key, Object value) {
		internalMap.put(key, value);
	}
	
	public Object get(String key) {
		return get(key);
	}
	
	public boolean isDefined(String propertyName) {
		return internalMap.containsKey(propertyName);
	}
	
	// <<< INSERT >>>
	public java.lang.String getName() {
		return (java.lang.String) internalMap.get("name"); 
	}
	public void setName(java.lang.String name) {
		internalMap.put("name", name);
	}	
	public java.lang.String getCategory() {
		return (java.lang.String) internalMap.get("category"); 
	}
	public void setCategory(java.lang.String category) {
		internalMap.put("category", category);
	}	
	public java.lang.String getDescription() {
		return (java.lang.String) internalMap.get("description"); 
	}
	public void setDescription(java.lang.String description) {
		internalMap.put("description", description);
	}	
	public java.lang.String getTags() {
		return (java.lang.String) internalMap.get("tags"); 
	}
	public void setTags(java.lang.String tags) {
		internalMap.put("tags", tags);
	}	
	public java.util.Date getEffectiveDate() {
		return (java.util.Date) internalMap.get("effectiveDate"); 
	}
	public void setEffectiveDate(java.util.Date effectiveDate) {
		internalMap.put("effectiveDate", effectiveDate);
	}	
	public java.util.Date getExpirationDate() {
		return (java.util.Date) internalMap.get("expirationDate"); 
	}
	public void setExpirationDate(java.util.Date expirationDate) {
		internalMap.put("expirationDate", expirationDate);
	}	
	public java.lang.String getCreatedBy() {
		return (java.lang.String) internalMap.get("createdBy"); 
	}
	public void setCreatedBy(java.lang.String createdBy) {
		internalMap.put("createdBy", createdBy);
	}	
	public java.util.Date getCreatedOn() {
		return (java.util.Date) internalMap.get("createdOn"); 
	}
	public void setCreatedOn(java.util.Date createdOn) {
		internalMap.put("createdOn", createdOn);
	}	
	public java.lang.String getModifiedBy() {
		return (java.lang.String) internalMap.get("modifiedBy"); 
	}
	public void setModifiedBy(java.lang.String modifiedBy) {
		internalMap.put("modifiedBy", modifiedBy);
	}	
	public java.util.Date getModifyOn() {
		return (java.util.Date) internalMap.get("modifyOn"); 
	}
	public void setModifyOn(java.util.Date modifyOn) {
		internalMap.put("modifyOn", modifyOn);
	}	
	public java.lang.String getBuildPhase() {
		return (java.lang.String) internalMap.get("buildPhase"); 
	}
	public void setBuildPhase(java.lang.String buildPhase) {
		internalMap.put("buildPhase", buildPhase);
	}	
	public java.lang.String getValidateDT() {
		return (java.lang.String) internalMap.get("validateDT"); 
	}
	public void setValidateDT(java.lang.String validateDT) {
		internalMap.put("validateDT", validateDT);
	}	
	public java.lang.String getLob() {
		return (java.lang.String) internalMap.get("lob"); 
	}
	public void setLob(java.lang.String lob) {
		internalMap.put("lob", lob);
	}	
	public java.lang.String getUsregion() {
		return (java.lang.String) internalMap.get("usregion"); 
	}
	public void setUsregion(java.lang.String usregion) {
		internalMap.put("usregion", usregion);
	}	
	public java.lang.String getCountry() {
		return (java.lang.String) internalMap.get("country"); 
	}
	public void setCountry(java.lang.String country) {
		internalMap.put("country", country);
	}	
	public java.lang.String getCurrency() {
		return (java.lang.String) internalMap.get("currency"); 
	}
	public void setCurrency(java.lang.String currency) {
		internalMap.put("currency", currency);
	}	
	public java.lang.String getLang() {
		return (java.lang.String) internalMap.get("lang"); 
	}
	public void setLang(java.lang.String lang) {
		internalMap.put("lang", lang);
	}	
	public java.lang.String getState() {
		return (java.lang.String) internalMap.get("state"); 
	}
	public void setState(java.lang.String state) {
		internalMap.put("state", state);
	}	
	public java.lang.String getRegion() {
		return (java.lang.String) internalMap.get("region"); 
	}
	public void setRegion(java.lang.String region) {
		internalMap.put("region", region);
	}	
	public java.lang.String getVersion() {
		return (java.lang.String) internalMap.get("version"); 
	}
	public void setVersion(java.lang.String version) {
		internalMap.put("version", version);
	}	
	public java.lang.Boolean getActive() {
		return (java.lang.Boolean) internalMap.get("active"); 
	}
	public void setActive(java.lang.Boolean active) {
		internalMap.put("active", active);
	}	
	public java.lang.Boolean getFailOnMiss() {
		return (java.lang.Boolean) internalMap.get("failOnMiss"); 
	}
	public void setFailOnMiss(java.lang.Boolean failOnMiss) {
		internalMap.put("failOnMiss", failOnMiss);
	}	
	public java.lang.Boolean getReturnOnMiss() {
		return (java.lang.Boolean) internalMap.get("returnOnMiss"); 
	}
	public void setReturnOnMiss(java.lang.Boolean returnOnMiss) {
		internalMap.put("returnOnMiss", returnOnMiss);
	}	
	public java.lang.String getScope() {
		return (java.lang.String) internalMap.get("scope"); 
	}
	public void setScope(java.lang.String scope) {
		internalMap.put("scope", scope);
	}	
	// <<< END INSERT >>>
	
}
