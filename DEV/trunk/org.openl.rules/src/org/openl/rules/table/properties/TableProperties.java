package org.openl.rules.table.properties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;

public class TableProperties extends DynamicObject implements ITableProperties {
    
    /**
     * Table section that contains properties in appropriate table in data source.
     */
    private ILogicalTable propertySection;
    
    /**
     * Collection, to mark all properties that were set by default for current instance.
     * Contains it names.
     */
    private Set<String> propertiesWithDefaultValue = new HashSet<String>();    
        
	@Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(getClass());
    }
	
    @Override
    public void setType(IOpenClass type) {
        throw new UnsupportedOperationException();
    }

    // <<< INSERT >>>
	public java.lang.String getName() {
		return (java.lang.String) getFieldValue("name"); 
	}
	public void setName(java.lang.String name) {
		setFieldValue("name", name);
	}	
	public java.lang.String getCategory() {
		return (java.lang.String) getFieldValue("category"); 
	}
	public void setCategory(java.lang.String category) {
		setFieldValue("category", category);
	}	
	public java.lang.String getDescription() {
		return (java.lang.String) getFieldValue("description"); 
	}
	public void setDescription(java.lang.String description) {
		setFieldValue("description", description);
	}	
	public java.lang.String getTags() {
		return (java.lang.String) getFieldValue("tags"); 
	}
	public void setTags(java.lang.String tags) {
		setFieldValue("tags", tags);
	}	
	public java.util.Date getEffectiveDate() {
		return (java.util.Date) getFieldValue("effectiveDate"); 
	}
	public void setEffectiveDate(java.util.Date effectiveDate) {
		setFieldValue("effectiveDate", effectiveDate);
	}	
	public java.util.Date getExpirationDate() {
		return (java.util.Date) getFieldValue("expirationDate"); 
	}
	public void setExpirationDate(java.util.Date expirationDate) {
		setFieldValue("expirationDate", expirationDate);
	}	
	public java.lang.String getCreatedBy() {
		return (java.lang.String) getFieldValue("createdBy"); 
	}
	public void setCreatedBy(java.lang.String createdBy) {
		setFieldValue("createdBy", createdBy);
	}	
	public java.util.Date getCreatedOn() {
		return (java.util.Date) getFieldValue("createdOn"); 
	}
	public void setCreatedOn(java.util.Date createdOn) {
		setFieldValue("createdOn", createdOn);
	}	
	public java.lang.String getModifiedBy() {
		return (java.lang.String) getFieldValue("modifiedBy"); 
	}
	public void setModifiedBy(java.lang.String modifiedBy) {
		setFieldValue("modifiedBy", modifiedBy);
	}	
	public java.util.Date getModifyOn() {
		return (java.util.Date) getFieldValue("modifyOn"); 
	}
	public void setModifyOn(java.util.Date modifyOn) {
		setFieldValue("modifyOn", modifyOn);
	}	
	public java.lang.String getBuildPhase() {
		return (java.lang.String) getFieldValue("buildPhase"); 
	}
	public void setBuildPhase(java.lang.String buildPhase) {
		setFieldValue("buildPhase", buildPhase);
	}	
	public java.lang.String getValidateDT() {
		return (java.lang.String) getFieldValue("validateDT"); 
	}
	public void setValidateDT(java.lang.String validateDT) {
		setFieldValue("validateDT", validateDT);
	}	
	public java.lang.String getLob() {
		return (java.lang.String) getFieldValue("lob"); 
	}
	public void setLob(java.lang.String lob) {
		setFieldValue("lob", lob);
	}	
	public java.lang.String getUsregion() {
		return (java.lang.String) getFieldValue("usregion"); 
	}
	public void setUsregion(java.lang.String usregion) {
		setFieldValue("usregion", usregion);
	}	
	public java.lang.String getCountry() {
		return (java.lang.String) getFieldValue("country"); 
	}
	public void setCountry(java.lang.String country) {
		setFieldValue("country", country);
	}	
	public java.lang.String getCurrency() {
		return (java.lang.String) getFieldValue("currency"); 
	}
	public void setCurrency(java.lang.String currency) {
		setFieldValue("currency", currency);
	}	
	public java.lang.String getLang() {
		return (java.lang.String) getFieldValue("lang"); 
	}
	public void setLang(java.lang.String lang) {
		setFieldValue("lang", lang);
	}	
	public java.lang.String getState() {
		return (java.lang.String) getFieldValue("state"); 
	}
	public void setState(java.lang.String state) {
		setFieldValue("state", state);
	}	
	public java.lang.String getRegion() {
		return (java.lang.String) getFieldValue("region"); 
	}
	public void setRegion(java.lang.String region) {
		setFieldValue("region", region);
	}	
	public java.lang.String getVersion() {
		return (java.lang.String) getFieldValue("version"); 
	}
	public void setVersion(java.lang.String version) {
		setFieldValue("version", version);
	}	
	public java.lang.Boolean getActive() {
		return (java.lang.Boolean) getFieldValue("active"); 
	}
	public void setActive(java.lang.Boolean active) {
		setFieldValue("active", active);
	}	
	public java.lang.Boolean getFailOnMiss() {
		return (java.lang.Boolean) getFieldValue("failOnMiss"); 
	}
	public void setFailOnMiss(java.lang.Boolean failOnMiss) {
		setFieldValue("failOnMiss", failOnMiss);
	}	
	public java.lang.Boolean getReturnOnMiss() {
		return (java.lang.Boolean) getFieldValue("returnOnMiss"); 
	}
	public void setReturnOnMiss(java.lang.Boolean returnOnMiss) {
		setFieldValue("returnOnMiss", returnOnMiss);
	}	
	public java.lang.String getScope() {
		return (java.lang.String) getFieldValue("scope"); 
	}
	public void setScope(java.lang.String scope) {
		setFieldValue("scope", scope);
	}	
	// <<< END INSERT >>>
	
    public Object getPropertyValue(String key) {        
        return getFieldValue(key);
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
    
    public void setPropertiesSection(ILogicalTable propertySection) {
        this.propertySection = propertySection;
     }
        
    public int getNumberOfProperties() {
        return getFieldValues().size();
    }
        
    public Map<String, Object> getPropertiesAll() {
      return  super.getFieldValues();
    }
    
    public void setDefaultPropertyValue(String propertyName, Object defaultValue) {
        super.setFieldValue(propertyName, defaultValue);
        
        //mark that property value is default
        propertiesWithDefaultValue.add(propertyName);
    }
        
    @Override
    public void setFieldValue(String name, Object value) {
        //we need to remove property that can be set by default 
        propertiesWithDefaultValue.remove(name);
        super.setFieldValue(name, value);
    }
        
    public Set<String> getPropertiesSetByDefault() {        
        return propertiesWithDefaultValue;
    }
    
    
    public Map<String, Object> getPropertiesIgnoreDefault() {    
        Map<String, Object> propWithoutDefault = new HashMap<String, Object>();        
        Map<String, Object> allProps = getPropertiesAll();
        Set<String> defaultProps = getPropertiesSetByDefault();        
        for (Entry<String, Object> property : allProps.entrySet()) {
            if (!defaultProps.contains(property.getKey())) {
                propWithoutDefault.put(property.getKey(), property.getValue());
            }
        }        
        return propWithoutDefault;
    }
    
    public Map<String, Object> getPropertiesIgnoreDefaultAndSystem() {
        Map<String, Object> result = new HashMap<String, Object>();
        Map<String, Object> propWithoutDefault = getPropertiesIgnoreDefault();
        for (Map.Entry<String, Object> property : propWithoutDefault.entrySet()) {
            String propName = property.getKey();
            TablePropertyDefinition propertyDefinition = DefaultPropertyDefinitions.getPropertyByName(propName);
            if (!propertyDefinition.isSystem()) {
                result.put(propName, property.getValue());                
            }
        }
        return result;
    }
	
}
