/**
 * 
 */
package com.exigen.le.smodel;

/**
 * Property that can be derived from other 
 * @author vabramovs
 *
 */
public class MappedProperty extends Property {
	private String mappingProperty;
	private String propertyRetriever=null;
	
	public MappedProperty(){
		super();
	}
	public MappedProperty(String name, Type type){
		super(name, type);
	}
	
	public MappedProperty (String name, Type type, boolean embedded){
		super(name, type, embedded);
	}

	public MappedProperty (String name, Type type, boolean embedded, boolean collection){
		super(name, type, embedded, collection);
	}	

//	private String propertySetter;
	
//	/**
//	 * @return the propertySetter
//	 */
//	public String getPropertySetter() {
//		return propertySetter;
//	}
//	/**
//	 * @param propertySetter the propertySetter to set
//	 */
//	public void setPropertySetter(String propertySetter) {
//		this.propertySetter = propertySetter;
//	}
	public String getMappingProperty() {
		return mappingProperty;
	}
	public void setMappingProperty(String mappingProperty) {
		this.mappingProperty = mappingProperty;
	}
	public String getPropertyRetriever() {
		return propertyRetriever;
	}
	public void setPropertyRetriever(String propertyRetriever) {
		this.propertyRetriever = propertyRetriever;
	}
}
