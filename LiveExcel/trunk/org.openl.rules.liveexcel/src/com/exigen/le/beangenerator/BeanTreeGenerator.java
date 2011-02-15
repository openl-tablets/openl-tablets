/**
 * 
 */
package com.exigen.le.beangenerator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.log4j.Logger;

import com.exigen.le.smodel.Property;
import com.exigen.le.smodel.Type;

/**
 * Creates "tree" of bean classes based on description 
 * @author zsulkins
 *
 */
public class BeanTreeGenerator {
	
	public static final String ROOT_GENERATED = "com.exigen.le.generated.";
	public static final String ROOT_BEAN_PACKAGE = ROOT_GENERATED+"beans.";
//	public static final String ROOT_BEAN_PACKAGE = "";
	private static final Logger logger = Logger.getLogger(BeanTreeGenerator.class);

	
	/** 
	 * Load referenced class
	 * @param className
	 * @param type
	 * @param loader
	 * @param writer
	 * @param jar
	 * @return
	 */
	public static Class<?> loadBeanClasses(String className, Type type, GeneratorClassLoader loader,  PrintWriter writer, JarOutputStream jar){
		return generateLoadTree(ROOT_BEAN_PACKAGE+className, type, loader, writer, jar);
		
	}
	
	/**
	 * Load referenced class
	 * @param className
	 * @param type
	 * @param loader
	 * @return
	 */
	public static Class<?> loadBeanClasses(String className, Type type,GeneratorClassLoader loader){
		return loadBeanClasses(className, type, loader, null, null);
	}
		
		
		
	protected static Class<?> generateLoadTree(String fuflo, Type type, GeneratorClassLoader loader, PrintWriter writer, JarOutputStream jar){
			// generate bean
			// look for property classes 
			// if class is also bean - generate it 
		byte[] classDef;
//		System.out.println("Generate bean for "+type.getName());
		if (writer == null){
			classDef = BeanGenerator.generate(type);
		} else {
			classDef = BeanGenerator.generate( type, writer);
		}
		String className = BeanGenerator.getQualifiedType(type); 
		Class<?> result = loader.defineClass(className, classDef);
		if (jar != null){
			// add to jar
			String entryName = className.replace('.', '/');
			entryName = entryName+".class";
			JarEntry entry = new JarEntry(entryName);
			entry.setTime(System.currentTimeMillis());
			try {
				jar.putNextEntry(entry);
				jar.write(classDef);
			} catch (IOException ioe){
				logger.error("Failed to create jar file with classes", ioe);
				throw new RuntimeException("Failed to create jar file with classes", ioe);
			}
		}
		
		// generate for properties
		for (Property child: type.getChilds()){
			if(child.getType().isComplex()&& child.isEmbedded())
				// our type - generate
				generateLoadTree(child.getType().getName(), child.getType(), loader, writer, jar);
		}
		return result;
			
	}
	
//	/**
//	 * Converts model description tree to bean description
//	 * @param smItem
//	 * @return bean description tree
//	 */
//	public static BeanGenerator.ItemDescription modelToBean(Property  smItem){
//		
//		String name = smItem.getName();
//		Type type = smItem.getType();
//		boolean isCollection = smItem.isCollection();
//		List<BeanGenerator.ItemDescription> properties = new ArrayList<BeanGenerator.ItemDescription>();
//		for (Property prop: type.getChilds()){
//			properties.add(modelToBean(prop));
//		}
//		BeanGenerator.ItemDescription id = new ItemDescription(name, type.getName(), isCollection, properties);
//		return id;
//	}
	
	
}
