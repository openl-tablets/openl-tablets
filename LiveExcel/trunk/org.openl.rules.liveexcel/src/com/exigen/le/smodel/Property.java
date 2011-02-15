/**
 * 
 */
package com.exigen.le.smodel;

/**
 * Property that form Type
 * @author vabramovs
 *
 */
public class Property {
	private String name;
	private  Type type=null;
	private String typeName;   // Full qualified type name (for example Departament.person if person is embedded type)
	private boolean embedded = false;
	private boolean collection = false;
	private boolean key = false;
	
	
	public Property(){};
	public Property (String name, Type type){
		this.name = name.toUpperCase();
		this.type = type;
		this.typeName = type.getName();
	}
	
	public Property (String name, Type type, boolean embedded){
		this.name = name.toUpperCase();
		this.typeName = type.getName();
		this.type = type;
		this.embedded = embedded;
		if(embedded){
			this.type =type;
		}
	}

	public Property (String name, Type type, boolean embedded, boolean collection){
		this.name = name.toUpperCase();
		this.typeName = type.getName();
		this.type = type;
		this.embedded = embedded;
		this.collection = collection;
		if(embedded){
			this.type =type;
		}
	}
	
	/**
	 * @return the key
	 */
	public boolean isKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(boolean key) {
		this.key = key;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name.toUpperCase();
	}
	/**
	 * @return the type
	 */
	public Type getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(Type type) {
		this.type = type;
	}
	/**
	 * @return the typeName
	 */
	public String getTypeName() {
		return typeName;
	}
	/**
	 * @param typeName the typeName to set
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName.toUpperCase();
	}

	/**
	 * @return the embedded
	 */
	public boolean isEmbedded() {
		return embedded;
	}
	/**
	 * @param embedded the embedded to set
	 */
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}
	/**
	 * @return the collection
	 */
	public boolean isCollection() {
		return collection;
	}
	/**
	 * @param colllection the collection to set
	 */
	public void setCollection(boolean colllection) {
		this.collection = colllection;
	}
	

}
