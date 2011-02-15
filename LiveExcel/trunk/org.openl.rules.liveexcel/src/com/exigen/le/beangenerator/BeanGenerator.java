package com.exigen.le.beangenerator;

import java.io.PrintWriter;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.TraceClassVisitor;

import com.exigen.le.smodel.Property;

/**
 * Generate bytecode representation (.class) of the bean
 * @author zsulkins
 *
 */
/**
 * @author zsulkins
 *
 */
public class BeanGenerator implements Opcodes {
	
	/**
	 * Generate bytecode representation (.class) of the bean
	 * @param className full name of the bean e.g. sample.test.Bean1
	 * @param descr description of the bean properties
	 * @return
	 */
	
	public static byte[] generate(com.exigen.le.smodel.Type type){
		ClassWriter cw = new ClassWriter(true);
		generate( type, cw);
		return cw.toByteArray();
	}

	/**
	 * Generate bytecode representation (.class) of the bean
	 * @param className full name of the bean e.g. sample.test.Bean1
	 * @param descr description of the bean properties
	 * @param printWriter dumps source code to this writer. Could be useful for debugging 
	 * @return
	 */
	
	public static byte[] generate(com.exigen.le.smodel.Type type, PrintWriter printWriter){
		ClassWriter cw = new ClassWriter(true);
		TraceClassVisitor tc = new TraceClassVisitor(cw, printWriter);
		generate(type, tc);
		return cw.toByteArray();
	}
	
	
	
	/**
	 * Generate bytecode representation (.class) of the bean
	 * 
	 * @param className full name of the bean e.g. sample.test.Bean1
	 * @param descr description of the bean properties
	 * @param cv ClassVisitor on which all generation happens 
	 * 
	 */
	protected static void generate(com.exigen.le.smodel.Type letype, ClassVisitor cv){
		
		FieldVisitor fv;
		MethodVisitor mv;

		String className = getQualifiedType (letype);
		
		if (className==null || className.length() == 0){
			throw new IllegalArgumentException("Class name should not be null or empty");
		}
		if (letype.getChilds() == null || letype.getChilds().size() == 0){
			throw new IllegalArgumentException("description should not be null or empty");
		}
		
		String slashedName = className.replace('.', '/');
		
		// create class description
		cv.visit(V1_5, ACC_PUBLIC + ACC_SUPER, slashedName, null, "java/lang/Object", null);

		// generate fields
		for (Property prop:letype.getChilds()){
			fv = cv.visitField(ACC_PRIVATE, prop.getName(), typeForGen(prop.getType(), prop.isCollection()).getDescriptor(), null, null);
			fv.visitEnd();
		}
		
		// generate empty constructor
		mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();

		
		
		// generate methods - set() and get()
		for (Property prop:letype.getChilds()){
			
			
			Type type = typeForGen(prop.getType(), prop.isCollection());
			// first letter of the property name should be capitalized
			String name = prop.getName().substring(0,1).toUpperCase()+prop.getName().substring(1);

			// getter
			mv = cv.visitMethod(ACC_PUBLIC,"get"+name ,"()"+type.getDescriptor(), null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, slashedName, prop.getName(), type.getDescriptor());
			mv.visitInsn(type.getOpcode(IRETURN));
			mv.visitMaxs(type.getSize(), 1);
			mv.visitEnd();
			
			// setter
			mv = cv.visitMethod(ACC_PUBLIC, "set"+name, "(" + type.getDescriptor() +  ")V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(type.getOpcode(ILOAD), 1);
			mv.visitFieldInsn(PUTFIELD, slashedName, prop.getName(), type.getDescriptor());
			mv.visitInsn(RETURN);
			mv.visitMaxs(1+type.getSize(), 1+type.getSize());
			mv.visitEnd();
			
			if (prop.isCollection()) {
				// generate indexed getter/setter
				type = typeForGen(prop.getType(), false); // working with array items
				
				// getter
				mv = cv.visitMethod(ACC_PUBLIC, "get"+name, "(I)"+type.getDescriptor(), null, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, slashedName,prop.getName(), "["+type.getDescriptor());
				mv.visitVarInsn(ILOAD, 1);
				mv.visitInsn(type.getOpcode(IALOAD));
				mv.visitInsn(type.getOpcode(IRETURN));
				mv.visitMaxs(2, 2);
				mv.visitEnd();
				
				// setter
				mv = cv.visitMethod(ACC_PUBLIC, "set"+name, "(I"+type.getDescriptor()+ ")V", null, null);
				mv.visitCode();
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, slashedName, prop.getName(), "["+type.getDescriptor());
				mv.visitVarInsn(ILOAD, 1);
				mv.visitVarInsn(type.getOpcode(ILOAD), 2);
				mv.visitInsn(type.getOpcode(IASTORE));
				mv.visitInsn(RETURN);
				mv.visitMaxs(2+type.getSize(), 2+type.getSize());
				mv.visitEnd();
				
			}
			
		}
		
		cv.visitEnd();
		
		
	}
	
	/**
	 * 
	 * @param provided type. Should be in form java.lang.String or mypackage.MyClass if it is class
	 * or constant ("double" etc.) if type is primitive java type
	 * @param isCollection
	 * @return type in JVM format
	 */
	private static Type typeForGen(com.exigen.le.smodel.Type type, boolean isCollection){
		String retVal="";
		if(!type.isComplex()){
			// Atomar
			String typeName = com.exigen.le.smodel.Type.Primary.getPrimary(type).getJavaClass().getCanonicalName();
			if (typeName.indexOf(".")==(-1)){
				// primitive type
				if (typeName.equals("boolean".toUpperCase()))
					retVal = "Z";
				if (typeName.equals("char".toUpperCase()))
					retVal = "C";
				if (typeName.equals("byte".toUpperCase()))
					retVal = "B";
				if (typeName.equals("short".toUpperCase()))
					retVal = "S";
				if (typeName.equals("int".toUpperCase()))
					retVal = "I";
				if (typeName.equals("float".toUpperCase()))
					retVal = "F";
				if (typeName.equals("long".toUpperCase()))
					retVal = "J";
				if (typeName.equals("double".toUpperCase()))
					retVal = "D";
				if (retVal.equals(""))
					throw new IllegalArgumentException("Primitive type " + type.getName() + " is not accepted" );
			} else {
				//  Java class
				retVal ="L"+getQualifiedType(type).replace('.', '/')+';'; //Ljava/lang/String;
			}
		}
		 else {
		// Our class
		retVal ="L"+getQualifiedType(type).replace('.', '/')+';'; //Ljava/lang/String;
		 }
		if (isCollection){
			retVal = '['+retVal;
		}
		return Type.getType(retVal);
	}
	/**
	 * Return fully qualified  type (with package)
	 * @param type
	 * @return
	 */
	public static String getQualifiedType (com.exigen.le.smodel.Type type){
		if(type.isComplex()){
			return BeanTreeGenerator.ROOT_BEAN_PACKAGE+type.getPath();
		}
		else{
			return com.exigen.le.smodel.Type.Primary.getPrimary(type).getJavaClass().getCanonicalName();
		}
	}
	
	
//	/**
//	 * Description of the bean property
//	 * @author zsulkins
//	 *
//	 */
//	public static class ItemDescription{
//		public String name;
//		public String type;
//		public boolean isCollection;
//		public List<ItemDescription> properties;
//		
//		/**
//		 * @param name property name
//		 * @param type property type, e.g. double, java.lang.String
//		 * @param isCollection true if the property is array
//		 */
//		public ItemDescription(String name, String type, boolean isCollection){
//			this(name, type, isCollection, (List<ItemDescription>)null);
//		}
//		
//		/**
//		 * @param name property name
//		 * @param type property type, e.g. double, java.lang.String
//		 * @param isCollection true if the property is array
//		 */
//		public ItemDescription(String name, String type, boolean isCollection, List<ItemDescription> properties){
//			this.name = name;
//			this.type = type;
//			this.isCollection = isCollection;
//			this.properties = properties;
//		}
//		
//	}
}
