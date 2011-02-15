package com.exigen.le.beangenerator;

import java.util.HashMap;


/**
 * @author vabramovs
 *
 */
public class GeneratorClassLoader extends ClassLoader {
	
	HashMap<String,byte[]> classDef = new HashMap<String, byte[]>();
	HashMap<String, Package> packageDef = new HashMap<String, Package>();
	
	@SuppressWarnings("unchecked")
	public Class defineClass(String name, byte[]b){
		if (name.indexOf('.') !=-1){ // define package
			int k = name.lastIndexOf('.');
			String packageName = name.substring(0, k);
			if (getPackage(packageName) == null){
				Package pkg = definePackage(packageName, null, null, null, null, null, null, null);
				packageDef.put(packageName, pkg);
			}
		}
		
		return defineClass(name,b,0,b.length);
	}
	
	/** Add Definition for Class
	 * @param className
	 * @param definition
	 */
	public void addDefinition(String className, byte[] definition){
		if (classDef.containsKey(className)){
			throw new IllegalArgumentException("class with this name already defined:" + className);
		}	
		classDef.put(className,definition);
	}
	
	/**
	 * Get class Loader
	 * 
	 */
	public GeneratorClassLoader(){
		super(GeneratorClassLoader.class.getClassLoader());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class findClass(String name) throws ClassNotFoundException{
		try {
			if (classDef.containsKey(name)){
				defineClass(name, classDef.get(name));
			}
		} catch (Exception e){
			throw new ClassNotFoundException("failed to instantiate in my Classloader", e);
		}
	return super.findClass(name);
	}
	
	@Override
	protected Package getPackage(String name){
		Package result = packageDef.get(name);
		if (result !=null){
			return result;
		}
		return super.getPackage(name);
	}
}
