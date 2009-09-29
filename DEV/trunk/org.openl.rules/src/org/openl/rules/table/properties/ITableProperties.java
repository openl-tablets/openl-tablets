package org.openl.rules.table.properties;

public interface ITableProperties {
	
	boolean isDefined(String propertyName);
	
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
	// <<< END INSERT >>>
	
}
