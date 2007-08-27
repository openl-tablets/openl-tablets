/**
 * Created Oct 25, 2005
 */
package org.openl.conf.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.main.OpenLWrapper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.FileTool;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class JavaWrapperAntTask extends Task
{

	public void execute() throws BuildException
	{
		try {
			run();
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	IOpenClass makeOpenClass() throws Exception
	{

		UserContext ucxt = new UserContext(Thread.currentThread()
				.getContextClassLoader(), userHome);
		if (userClassPath != null) {
			ClassLoader cl = ClassLoaderFactory.createClassLoader(userClassPath, this
					.getClass().getClassLoader(), ucxt);

			ucxt = new UserContext(cl, userHome);
		}
		OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(
				openlName, ucxt, srcFile);
		return wrapper.getOpenClass();

	}

	String getOutputFileName()
	{
		String file = targetSrcDir + "/" + targetClass.replace('.', '/') + ".java";
		return file;
	}

	IOpenClass openClass;
	
	void run() throws Exception
	{
		if (ignoreFields != null)
			fields = StringTool.tokenize(ignoreFields, ", ");

		if (ignoreMethods != null)
			methods = StringTool.tokenize(ignoreMethods, ", ");
		
		
		saveProjectProperties();
		
		openClass = makeOpenClass();
		String content = generateJavaClass(openClass);
		FileWriter fw = null;
		try {
			String fileName = getOutputFileName();
			new File(fileName).getParentFile().mkdirs();
			fw = new FileWriter(fileName);
			fw.write(content);
		} finally {
			if (fw != null)
				fw.close();
		}
	}

	/**
	 * @throws IOException 
	 * 
	 */
	private void saveProjectProperties() throws IOException 
	{
		Properties p = new Properties();
		p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_PROPERTY, 
				filterClassPath());
		
		if (displayName != null)
			p.put(targetClass + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, displayName);
		
		new OpenLProjectPropertiesLoader().saveProperties(".", p, false);
	}
	
	String classpathExclude = ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*"; 

	String projectHome = ".";
	
	/**
	 * @return
	 * @throws IOException 
	 */
	private String filterClassPath() throws IOException
	{
		String cp = System.getProperty("java.class.path");

		String[] tokens = StringTool.tokenize(cp, File.pathSeparator); 
		
		StringBuffer buf = new StringBuffer(300);
		
		for (int i = 0; i < tokens.length; i++)
		{
			if (tokens[i].matches(classpathExclude)) continue;
			File f = FileTool.buildRelativePath(new File(projectHome), new File(tokens[i]));
			String relativePath = f.getPath().replace('\\', '/'); 
			buf.append(relativePath).append(File.pathSeparator);
		}
		return buf.toString();
	}

	boolean ignoreNonJavaTypes = false;
	String ignoreFields;
	String ignoreMethods;
	
	String userClassPath;

	String userHome, deplUserHome;

	String srcFile, deplSrcFile;

	String openlName;

	String targetSrcDir;

	String targetClass;
	
	String displayName;

	String[] methods;

	String[] fields;

	String s_package;

	String s_class;

	String s_extends = null;

	String s_implements = OpenLWrapper.class.getName();

	StringBuffer initBuf = new StringBuffer(1000);

	protected void addComment(StringBuffer buf)
	{
		buf.append(
"/*\n" +
" * This class has been generated. Do not change it, if you need to modify functionality - subclass it\n"+
" */\n\n"
		
		);
	}
	
	
	protected void addPackage(StringBuffer buf)
	{
		if (s_package != null)
			buf.append("package " + s_package + ";\n\n");
	}

	protected void addClassDeclaration(StringBuffer buf)
	{
		buf.append("public class " + s_class);
		if (s_extends != null)
			buf.append(" extends ").append(s_extends);
		if (s_implements != null)
			buf.append(" implements ").append(s_implements);
		buf.append("\n{\n");
	}


	int calcMethods(IOpenClass ioc)
	{
		int cnt = 0;
		
		for (Iterator iter = ioc.methods(); iter.hasNext();) 
		{
			IOpenMethod method = (IOpenMethod) iter.next();
			if (!isMethodGenerated(method))
				continue;
			++cnt;
		}
		
		return cnt;
	}
	
	public String generateJavaClass(IOpenClass ioc)
	{
		StringBuffer buf = new StringBuffer(10000);

		parseClassName();
		
		addComment(buf);

		addPackage(buf);

		int cnt = calcMethods(ioc);
		if (cnt != 0)
		{
			addImport(buf, "org.openl.util.Log");
			addImport(buf, "org.openl.util.RuntimeExceptionWrapper");
			addImport(buf, "org.openl.types.java.JavaOpenClass");
		}
		
		addImport(buf, "org.openl.types.IOpenClass");
		addImport(buf, "org.openl.conf.UserContext");


		addImport(buf, "org.openl.impl.OpenClassJavaWrapper");

		addClassDeclaration(buf);

		buf.append("  Object __instance;\n\n");

		buf.append("  public static org.openl.vm.IRuntimeEnv __env;\n\n");

		buf.append("  public static org.openl.types.IOpenClass __class;\n\n");

		buf.append("  public static org.openl.CompiledOpenClass __compiledClass;\n\n");

		
		buf.append("  public static String __openlName = \"" + openlName + "\";\n\n");

		buf.append("  public static String __src = \""
				+ (deplSrcFile == null ? srcFile : deplSrcFile) + "\";\n\n");

		buf.append("  public static String __userHome = \""
				+ (deplUserHome == null ? userHome : deplUserHome) + "\";\n\n");

		buf.append(
					"  public " + s_class + "(){\n" 
				+ "    this(false);\n"
				+ "  }\n\n");

		buf.append(
				"  public " + s_class + "(boolean ignoreErrors){\n" 
			+ "    __init();\n"
	    + "    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();\n"
			+ "    __instance = __class.newInstance(__env);\n"
			+ "  }\n\n");
		

		
		
		for (Iterator iter = ioc.fields(); iter.hasNext();) {
			IOpenField field = (IOpenField) iter.next();
			if (!isFieldGenerated(field))
				continue;
			addFieldFieldInitializer(field);
			addFieldField(field, buf);
			addFieldAccessor(field, buf);
			addFieldModifier(field, buf);
		}

		for (Iterator iter = ioc.methods(); iter.hasNext();) {
			IOpenMethod method = (IOpenMethod) iter.next();
			if (!isMethodGenerated(method))
				continue;
			addMethodFieldInitializer(method);
			addMethodField(method, buf);
			addMethodAccessor(method, buf, isStatic(method));
		}

		addInitMethod(buf);

		buf.append("}");

		return buf.toString();
	}

	/**
	 * @param method
	 * @return
	 */
	private boolean isStatic(IOpenMethod method)
	{
		return method.getName().equals("main")
				&& method.getSignature().getParameterTypes().length == 1
				&& method.getSignature().getParameterTypes()[0].getInstanceClass()
						.equals(String[].class);
	}

	/**
	 * @param buf
	 * @param string
	 */
	private void addImport(StringBuffer buf, String str)
	{
		buf.append("import ").append(str).append(";\n");

	}

	/**
	 * 
	 */
	private void parseClassName()
	{
		int idx = targetClass.lastIndexOf('.');
		if (idx < 0)
			s_class = targetClass;
		else {
			s_package = targetClass.substring(0, idx);
			s_class = targetClass.substring(idx + 1, targetClass.length());
		}
	}
	static String resName = "__res";

	/**
	 * @param field
	 * @param buf
	 */
	private void addFieldAccessor(IOpenField field, StringBuffer buf)
	{
		// public int getAbc()
		// {
		//
		// Object __res = abc_Field.get(__instance, __env);
		//
		// return ((Integer) __res).intValue();
		// }


		IOpenClass type = field.getType();

		String className = getClassName(type.getInstanceClass());

		buf.append("\n  public ").append(className).append(" get").append(
				fieldMethodPart(field)).append("()").append("\n  {\n").append(
				"   Object ").append(resName).append(" = ").append(
				getFieldFieldName(field)).append(".get(__instance, __env);\n").append(
				"   return ").append(castAndUnwrap(type.getInstanceClass(), resName))
				.append(";\n").append("  }\n\n");

	}

	private void addFieldModifier(IOpenField field, StringBuffer buf)
	{
		// public void setAbc(int x)
		// {
		//
		// abc_Field.set(__instance, new Integer(x) , __env);
		// }
		//    
		String varname = "__var";
		IOpenClass type = field.getType();

		String className = getClassName(type.getInstanceClass());

		buf.append("\n  public void set").append(fieldMethodPart(field))
				.append("(").append(className).append(' ').append(varname).append(")")
				.append("\n  {\n").append("   ").append(getFieldFieldName(field))
				.append(".set(__instance, ").append(
						wrapIfPrimitive(varname, type.getInstanceClass())).append(
						", __env);\n").append("  }\n\n");

	}

	/**
	 * @param field
	 * @return
	 */
	private String fieldMethodPart(IOpenField field)
	{
		String name = field.getName();
		return StringTool.capitalize(name);

	}

	/**
	 * @param buf
	 */
	protected void addInitMethod(StringBuffer buf)
	{

		String initStart =

		      "  static boolean __initialized = false;\n\n"
			  + "  static public void reset(){__initialized = false;}\n\n"
			  
				+ "public Object getInstance(){return __instance;}\n\n"


				+ "public IOpenClass getOpenClass(){return __class;}\n\n"

				+ "public org.openl.CompiledOpenClass getCompiledOpenClass(){return __compiledClass;}\n\n"
				

				+ "public synchronized void  reload(){reset();__init();__instance = __class.newInstance(__env);}\n\n"
			  
			  
			  
				+ "  static synchronized protected void __init()\n"
				+ "  {\n"
				+ "    if (__initialized)\n"
				+ "      return;\n\n"
				+

				"    UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), __userHome);\n"
				+ "    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src);\n"
				
		    + "    __compiledClass = wrapper.getCompiledClass();\n"
		    + "    __class = wrapper.getOpenClassWithErrors();\n"
				
				+ "    __env = wrapper.getEnv();\n\n";

		buf.append(initStart).append(initBuf.toString()).append(
				"\n    __initialized=true;\n  }\n");
	}

	protected boolean isFieldGenerated(IOpenField field)
	{
		if (fields != null && !ArrayTool.contains(fields, field.getName()))
			return false;

		IOpenClass type = field.getType();
		if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass))
			return false;

		return true;
	}

	protected boolean isMethodGenerated(IOpenMethod method)
	{
		
		// TODO fix a) provide isConstructor() in OpenMethod b) provide better name for XLS modules
		if (openClass.getName().equals(method.getName()))
			return false;
		
		if ("getOpenClass".equals(method.getName()))
			return false;

		if (methods != null && !ArrayTool.contains(methods, method.getName()))
			return false;

		IOpenClass type = method.getType();
		if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass))
			return false;

		IOpenClass[] params = method.getSignature().getParameterTypes();
		for (int i = 0; i < params.length; i++) {
			type = params[i];
			if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass))
				return false;

		}
		return true;
	}

	/**
	 * @param method
	 */
	public void addMethodFieldInitializer(IOpenMethod method)
	{
		// XYZ_Method = __class.getMatchingMethod("XYZ", new IOpenClass[] {
		// JavaOpenClass.getOpenClass(int.class),
		// JavaOpenClass.getOpenClass(String.class) });

		initBuf.append("    " + getMethodFieldName(method)
				+ " = __class.getMatchingMethod(\"" + method.getName()
				+ "\", new IOpenClass[] {\n");

		IOpenClass[] params = method.getSignature().getParameterTypes();

		for (int i = 0; i < params.length; i++) {
			if (i > 0)
				initBuf.append(",\n");

			// JavaOpenClass.getOpenClass(int.class),
			initBuf.append("      JavaOpenClass.getOpenClass(").append(
					getClassName(params[i].getInstanceClass())).append(".class)");
		}

		initBuf.append("});\n");
	}

	public void addFieldFieldInitializer(IOpenField field)
	{
		// abc_Field = __class.getField("abc");

		initBuf.append("    " + getFieldFieldName(field) + " = __class.getField(\""
				+ field.getName() + "\");\n");

	}

	public void addMethodField(IOpenMethod method, StringBuffer buf)
	{
		buf.append("\n\n  static " + IOpenMethod.class.getName() + " "
				+ getMethodFieldName(method) + ";\n");
	}

	public void addFieldField(IOpenField field, StringBuffer buf)
	{
		buf.append("\n\n  static " + IOpenField.class.getName() + " "
				+ getFieldFieldName(field) + ";\n");
	}

	public void addMethodAccessor(IOpenMethod method, StringBuffer buf,
			boolean isStatic)
	{
		addMethodSignature(method, buf, isStatic);
		addMethodBody(method, buf, isStatic);
	}

	public void addMethodBody(IOpenMethod method, StringBuffer buf,
			boolean isStatic)
	{
		buf.append("  {\n");

		IOpenClass[] ptypes = method.getSignature().getParameterTypes();

		// Object[] __params = new Object[2];

		buf.append("    Object[] __params = new Object[").append(ptypes.length)
				.append("];");

		// params[0] = new Integer[p1];
		// params[1] = p2;

		for (int i = 0; i < ptypes.length; i++) {
			buf.append("\n    __params[").append(i).append("] = ").append(
					parameterToObject(method, i)).append(';');
		}

		// try
		// {

		//    
		// Object res = XYZ_Method.invoke(instance, params, env);

		buf.append("\n    try\n    {\n");

		// Object instance = new Demo1().__instance;
		buf.append("    Object __myInstance = ");
		if (isStatic)
			buf.append("new " + this.s_class + "().");

		buf.append("__instance;\n");

		buf.append(returnMethodVar(method, resName)).append(getMethodFieldName(method))
				.append(".invoke(__myInstance, __params, __env);");
		//    
		// return ((Double)res).doubleValue();

		buf.append(returnMethodResult(method, resName));
		buf.append("  }\n" + "  catch(Throwable t)\n" + "  {\n"
				+ "    Log.error(\"Java Wrapper execution error:\", t);\n"
				+ "    throw RuntimeExceptionWrapper.wrap(t);\n" + "  }\n");

		buf.append("\n  }\n");
	}

public String returnMethodVar(IOpenMethod method, String resName)
{
	
	IOpenClass type = method.getType();

	Class instanceClass = type.getInstanceClass();
	if (instanceClass == void.class)
		return "    ";
	return   "    Object " + resName + " = "; 
}
	
	/**
	 * @param method
	 * @param string
	 * @return
	 */
	public String returnMethodResult(IOpenMethod method, String resName)
	{
		IOpenClass type = method.getType();

		Class instanceClass = type.getInstanceClass();
		if (instanceClass == void.class)
			return "";

		return "\n   return " + castAndUnwrap(instanceClass, resName) + ";";
	}

	/**
	 * @param instanceClass
	 * @param resName
	 * @return
	 */
	public String castAndUnwrap(Class instanceClass, String resName)
	{
		if (instanceClass == Object.class)
			return resName;

		if (instanceClass.isPrimitive())
			return unwrapIfPrimitive(instanceClass, resName);

		return "(" + getClassName(instanceClass) + ")" + resName;
	}

	/**
	 * @param instanceClass
	 * @param resName
	 * @return
	 */
	public String unwrapIfPrimitive(Class instanceClass, String name)
	{
		if (instanceClass == int.class)
			return "((Integer)" + name + ").intValue()";
		if (instanceClass == double.class)
			return "((Double)" + name + ").doubleValue()";
		if (instanceClass == boolean.class)
			return "((Boolean)" + name + ").booleanValue()";
		if (instanceClass == char.class)
			return "((Character)" + name + ").charValue()";
		if (instanceClass == long.class)
			return "((Long)" + name + ").longValue()";
		if (instanceClass == short.class)
			return "((Short)" + name + ").shortValue()";
		if (instanceClass == float.class)
			return "((Float)" + name + ").floatValue()";
		return name;
	}

	/**
	 * @param method
	 * @return
	 */
	public String getMethodFieldName(IOpenMethod method)
	{
		return getMethodName(method) + "_Method";
	}

	public String getFieldFieldName(IOpenField field)
	{
		return field.getName() + "_Field";
	}

	/**
	 * @param method
	 * @param i
	 * @return
	 */
	public String parameterToObject(IOpenMethod method, int i)
	{
		IOpenClass type = method.getSignature().getParameterTypes()[i];
		String name = getParamName(method.getSignature().getParameterName(i), i);

		Class instanceClass = type.getInstanceClass();
		if (instanceClass.isPrimitive())
			return wrapIfPrimitive(name, instanceClass);

		return name;
	}

	/**
	 * @param name
	 * @param instanceClass
	 * @return
	 */
	public String wrapIfPrimitive(String name, Class instanceClass)
	{
		if (instanceClass == int.class)
			return "new Integer(" + name + ")";
		if (instanceClass == double.class)
			return "new Double(" + name + ")";
		if (instanceClass == boolean.class)
			return "new Boolean(" + name + ")";
		if (instanceClass == char.class)
			return "new Character(" + name + ")";
		if (instanceClass == long.class)
			return "new Long(" + name + ")";
		if (instanceClass == short.class)
			return "new Short(" + name + ")";
		if (instanceClass == float.class)
			return "new Float(" + name + ")";

		return name;
	}

	public void addMethodSignature(IOpenMethod method, StringBuffer buf,
			boolean isStatic)
	{
		buf.append("  public ");
		if (isStatic)
			buf.append("static ");
		buf.append(getMethodType(method)).append(' ');
		buf.append(getMethodName(method));
		buf.append('(');
		IOpenClass[] ptypes = method.getSignature().getParameterTypes();
		for (int i = 0; i < ptypes.length; i++) {
			if (i > 0)
				buf.append(", ");
			buf.append(getOpenClassType(ptypes[i])).append(' ').append(
					getParamName(method.getSignature().getParameterName(i), i));
		}
		buf.append(')');
	}

	/**
	 * @param method
	 * @return
	 */
	public String getMethodName(IOpenMethod method)
	{
		return method.getName();
	}

	/**
	 * @param parameterName
	 * @return
	 */
	public String getParamName(String parameterName, int i)
	{
		return parameterName == null ? "arg" + i : parameterName;
	}

	public String getMethodType(IOpenMethod method)
	{
		return getOpenClassType(method.getType());
	}

	/**
	 * @param type
	 * @return
	 */
	public String getOpenClassType(IOpenClass type)
	{
		return getClassName(type.getInstanceClass());
	}

	/**
	 * @param instanceClass
	 * @return
	 */
	public String getClassName(Class instanceClass)
	{
		StringBuffer buf = new StringBuffer(30);
		while (instanceClass.isArray()) {
			buf.append("[]");
			instanceClass = instanceClass.getComponentType();
		}

		buf.insert(0, getScalarClassName(instanceClass));
		return buf.toString();
	}

	public String getScalarClassName(Class instanceClass)
	{
		return instanceClass.getName();
	}

	public String getDeplSrcFile()
	{
		return deplSrcFile;
	}

	public void setDeplSrcFile(String deplSrcFile)
	{
		this.deplSrcFile = deplSrcFile;
	}

	public String getDeplUserHome()
	{
		return deplUserHome;
	}

	public void setDeplUserHome(String deplUserHome)
	{
		this.deplUserHome = deplUserHome;
	}

	public String[] getFields()
	{
		return fields;
	}

	public void setFields(String[] fields)
	{
		this.fields = fields;
	}

	public String[] getMethods()
	{
		return methods;
	}

	public void setMethods(String[] methods)
	{
		this.methods = methods;
	}

	public String getOpenlName()
	{
		return openlName;
	}

	public void setOpenlName(String openlName)
	{
		this.openlName = openlName;
	}

	public String getS_class()
	{
		return s_class;
	}

	public void setS_class(String s_class)
	{
		this.s_class = s_class;
	}

	public String getS_extends()
	{
		return s_extends;
	}

	public void setS_extends(String s_extends)
	{
		this.s_extends = s_extends;
	}

	public String getS_implements()
	{
		return s_implements;
	}

	public void setS_implements(String s_implements)
	{
		this.s_implements = s_implements;
	}

	public String getS_package()
	{
		return s_package;
	}

	public void setS_package(String s_package)
	{
		this.s_package = s_package;
	}

	public String getSrcFile()
	{
		return srcFile;
	}

	public void setSrcFile(String srcFile)
	{
		this.srcFile = srcFile;
	}

	public String getTargetClass()
	{
		return targetClass;
	}

	public void setTargetClass(String targetClass)
	{
		this.targetClass = targetClass;
	}

	public String getTargetSrcDir()
	{
		return targetSrcDir;
	}

	public void setTargetSrcDir(String targetSrcDir)
	{
		this.targetSrcDir = targetSrcDir;
	}

	public String getUserClassPath()
	{
		return userClassPath;
	}

	public void setUserClassPath(String userClassPath)
	{
		this.userClassPath = userClassPath;
	}

	public String getUserHome()
	{
		return userHome;
	}

	public void setUserHome(String userHome)
	{
		this.userHome = userHome;
	}

	public String getIgnoreFields()
	{
		return ignoreFields;
	}

	public void setIgnoreFields(String ignoreFields)
	{
		this.ignoreFields = ignoreFields;
	}

	public String getIgnoreMethods()
	{
		return ignoreMethods;
	}

	public void setIgnoreMethods(String ignoreMethods)
	{
		this.ignoreMethods = ignoreMethods;
	}

	public boolean isIgnoreNonJavaTypes()
	{
		return ignoreNonJavaTypes;
	}

	public void setIgnoreNonJavaTypes(boolean ignoreNonJavaTypes)
	{
		this.ignoreNonJavaTypes = ignoreNonJavaTypes;
	}

	public String getDisplayName()
	{
		return this.displayName;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}

	public String getClasspathExclude()
	{
		return this.classpathExclude;
	}

	public void setClasspathExclude(String classpathExclude)
	{
		this.classpathExclude = classpathExclude;
	}
	
	
}
