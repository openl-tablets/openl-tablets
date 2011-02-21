/**
 * 
 */
package com.exigen.le.smodel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlTransient;

import com.exigen.le.LE_Value;



/**
 * Service Model Type
 * @author vabramovs
 *
 */
public class Type {
	
	public static final String DATE_FORMAT = "yyyy/MM/dd-HH:mm";
	public final static Type DOUBLE = new Type(LE_Value.TypeString.NUMERIC, false);
	public final static Type STRING = new Type(LE_Value.TypeString.STRING, false);
	public final static Type DATE = new Type(LE_Value.TypeString.DATE,false);
	public final static Type BOOLEAN = new Type(LE_Value.TypeString.BOOLEAN,false);
	
	
	private String name;
	private boolean complex;
	transient	private String path;
	private List<MappedProperty> childs = new ArrayList<MappedProperty>();
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
	 * @return the complex
	 */
	public boolean isComplex() {
		return complex;
	}
	/**
	 * @param complex the complex to set
	 */
	public void setComplex(boolean complex) {
		this.complex = complex;
	}
	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}
	/**
	 * @param path the path to set
	 */
	@XmlTransient
	public void setPath(String path) {
		this.path = path.toUpperCase();
	}
	/**
	 * @return the childs
	 */
	public List<MappedProperty> getChilds() {
		return  childs;
	}
	/**
	 * @param childs the childs to set
	 */
	public void setChilds(List<MappedProperty> childs) {
		this.childs = childs;
	}
	public void setPaths(String path) {
		this.path = path.toUpperCase();
		String prefix = path.isEmpty()?"":path+".";
		for(Property child:childs){
			if(child.isEmbedded())
				child.getType().setPaths(prefix+child.getName());
		}
	}

	public Type(){};
	
	public Type(String name, boolean complex){
		this.name = name.toUpperCase();
		this.complex = complex;
	}
	
	
	
	public List<String> getKeyList(){
		List<String> result = new ArrayList<String>();
		for(Property prop:childs){
			if(prop.isKey()){
				result.add(prop.getName());
			}
		}
		return result;
	}
	public boolean equals(Object obj) {
		if(!(obj instanceof Type)) return false;
		Type type = (Type)obj;
		if(this==type) return true;
		return this.name.equals(type.getName())&&this.complex==type.isComplex(); 
	}
	public int hashCode(){
		return name.hashCode();
	}
}
