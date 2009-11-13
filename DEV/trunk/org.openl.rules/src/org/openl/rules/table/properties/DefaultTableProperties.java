package org.openl.rules.table.properties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.impl.DynamicObject;

public class DefaultTableProperties extends DynamicObject implements ITableProperties {
    
    private ILogicalTable propertySection;

	// <<< INSERT >>>
	public java.lang.String getName() {
		return (java.lang.String) fieldValues.get("name"); 
	}
	public void setName(java.lang.String name) {
		fieldValues.put("name", name);
	}	
	public java.lang.String getCategory() {
		return (java.lang.String) fieldValues.get("category"); 
	}
	public void setCategory(java.lang.String category) {
		fieldValues.put("category", category);
	}	
	public java.lang.String getDescription() {
		return (java.lang.String) fieldValues.get("description"); 
	}
	public void setDescription(java.lang.String description) {
		fieldValues.put("description", description);
	}	
	public java.lang.String getTags() {
		return (java.lang.String) fieldValues.get("tags"); 
	}
	public void setTags(java.lang.String tags) {
		fieldValues.put("tags", tags);
	}	
	public java.util.Date getEffectiveDate() {
		return (java.util.Date) fieldValues.get("effectiveDate"); 
	}
	public void setEffectiveDate(java.util.Date effectiveDate) {
		fieldValues.put("effectiveDate", effectiveDate);
	}	
	public java.util.Date getExpirationDate() {
		return (java.util.Date) fieldValues.get("expirationDate"); 
	}
	public void setExpirationDate(java.util.Date expirationDate) {
		fieldValues.put("expirationDate", expirationDate);
	}	
	public java.lang.String getCreatedBy() {
		return (java.lang.String) fieldValues.get("createdBy"); 
	}
	public void setCreatedBy(java.lang.String createdBy) {
		fieldValues.put("createdBy", createdBy);
	}	
	public java.util.Date getCreatedOn() {
		return (java.util.Date) fieldValues.get("createdOn"); 
	}
	public void setCreatedOn(java.util.Date createdOn) {
		fieldValues.put("createdOn", createdOn);
	}	
	public java.lang.String getModifiedBy() {
		return (java.lang.String) fieldValues.get("modifiedBy"); 
	}
	public void setModifiedBy(java.lang.String modifiedBy) {
		fieldValues.put("modifiedBy", modifiedBy);
	}	
	public java.util.Date getModifyOn() {
		return (java.util.Date) fieldValues.get("modifyOn"); 
	}
	public void setModifyOn(java.util.Date modifyOn) {
		fieldValues.put("modifyOn", modifyOn);
	}	
	public java.lang.String getBuildPhase() {
		return (java.lang.String) fieldValues.get("buildPhase"); 
	}
	public void setBuildPhase(java.lang.String buildPhase) {
		fieldValues.put("buildPhase", buildPhase);
	}	
	public java.lang.String getValidateDT() {
		return (java.lang.String) fieldValues.get("validateDT"); 
	}
	public void setValidateDT(java.lang.String validateDT) {
		fieldValues.put("validateDT", validateDT);
	}	
	public java.lang.String getLob() {
		return (java.lang.String) fieldValues.get("lob"); 
	}
	public void setLob(java.lang.String lob) {
		fieldValues.put("lob", lob);
	}	
	public java.lang.String getUsregion() {
		return (java.lang.String) fieldValues.get("usregion"); 
	}
	public void setUsregion(java.lang.String usregion) {
		fieldValues.put("usregion", usregion);
	}	
	public java.lang.String getCountry() {
		return (java.lang.String) fieldValues.get("country"); 
	}
	public void setCountry(java.lang.String country) {
		fieldValues.put("country", country);
	}	
	public java.lang.String getCurrency() {
		return (java.lang.String) fieldValues.get("currency"); 
	}
	public void setCurrency(java.lang.String currency) {
		fieldValues.put("currency", currency);
	}	
	public java.lang.String getLang() {
		return (java.lang.String) fieldValues.get("lang"); 
	}
	public void setLang(java.lang.String lang) {
		fieldValues.put("lang", lang);
	}	
	public java.lang.String getState() {
		return (java.lang.String) fieldValues.get("state"); 
	}
	public void setState(java.lang.String state) {
		fieldValues.put("state", state);
	}	
	public java.lang.String getRegion() {
		return (java.lang.String) fieldValues.get("region"); 
	}
	public void setRegion(java.lang.String region) {
		fieldValues.put("region", region);
	}	
	public java.lang.String getVersion() {
		return (java.lang.String) fieldValues.get("version"); 
	}
	public void setVersion(java.lang.String version) {
		fieldValues.put("version", version);
	}	
	public java.lang.Boolean getActive() {
		return (java.lang.Boolean) fieldValues.get("active"); 
	}
	public void setActive(java.lang.Boolean active) {
		fieldValues.put("active", active);
	}	
	public java.lang.Boolean getFailOnMiss() {
		return (java.lang.Boolean) fieldValues.get("failOnMiss"); 
	}
	public void setFailOnMiss(java.lang.Boolean failOnMiss) {
		fieldValues.put("failOnMiss", failOnMiss);
	}	
	public java.lang.Boolean getReturnOnMiss() {
		return (java.lang.Boolean) fieldValues.get("returnOnMiss"); 
	}
	public void setReturnOnMiss(java.lang.Boolean returnOnMiss) {
		fieldValues.put("returnOnMiss", returnOnMiss);
	}	
	public java.lang.String getScope() {
		return (java.lang.String) fieldValues.get("scope"); 
	}
	public void setScope(java.lang.String scope) {
		fieldValues.put("scope", scope);
	}	
	// <<< END INSERT >>>
	
    public Object getPropertyValue(String key) {        
        return fieldValues.get(key);
    }
    
    public String getPropertyValueAsString(String key) {
        String result = null;
        Object propValue = getPropertyValue(key);
        if (propValue != null) {            
            if (propValue instanceof String) {
                result = (String) propValue;
            } else {
                if (propValue instanceof Date) {
                    String format = DefaultPropertyDefinitions
                            .getPropertyByName(key).getFormat();
                    if (format != null) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat(
                                format);
                        result = dateFormat.format((Date) propValue);
                    }
                } else {
                    if (propValue instanceof Boolean) {
                        result = ((Boolean) propValue).toString();
                    } else {
                        if (propValue instanceof Integer) {
                            result = ((Integer) propValue).toString();
                        }
                    }
                }
            }
        }
        return result;        
    }
    public ILogicalTable getPropertiesSection() {
       return propertySection;
    }
    public int getNumberOfProperties() {
        return fieldValues.size();
    }
    
    public Map<String, Object> getDefinedProperties() {
      return  super.getFieldValues();
    }
	
}
