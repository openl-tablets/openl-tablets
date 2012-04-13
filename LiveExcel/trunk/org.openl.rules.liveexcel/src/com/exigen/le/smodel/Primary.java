package com.exigen.le.smodel;

import com.exigen.le.LE_Value;

public enum Primary  {
	DOUBLE(Type.DOUBLE, "java.lang.Double"),
	STRING(Type.STRING, "java.lang.String"),
	DATE(Type.DATE, "java.util.Calendar"),
	BOOLEAN(Type.BOOLEAN, "java.lang.Boolean");
	
	private final Type type;
	private final Class<?> clazz;
	
	Primary(Type type, String javaClass){
		this.type = type;
		try {
			clazz = Class.forName(javaClass);
		} catch (ClassNotFoundException cnfe){
			throw new RuntimeException(cnfe);
		}
	}
	
	public Type getType(){
		return type;
	}
	
	public String getPrimaryName(){
		return type.getName();
	}
	
	public Class<?> getJavaClass(){
		return clazz;
	}
	
	public static Type getTypeByName(String name){
		for (Primary p: Primary.values()){
			if (p.type.getName().equalsIgnoreCase(name)){
				return p.type;
			}
		}
		return null;
	}
	
	public static Type getTypeByClass(String className){
		for (Primary p: Primary.values()){
			if (p.clazz.getCanonicalName().equals(className)){
				return p.type;
			}
		}
		return null;
	}
	
	public static Type getTypeByNameOrClass(String nameOrClassName){
		Type result = getTypeByName(nameOrClassName);
		if (result !=null){
			return result;
		}
		return getTypeByClass(nameOrClassName);
	}
	
	
	public static Primary getPrimary(Type type){
		for (Primary p: Primary.values()){
			if (p.type.equals(type)){
				return p;
			}
		}
		return null;
	}
	
}