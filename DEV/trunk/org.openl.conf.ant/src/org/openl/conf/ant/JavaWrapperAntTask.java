/**
 * Created Oct 25, 2005
 */
package org.openl.conf.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.main.OpenLWrapper;
import org.openl.meta.IVocabulary;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayTool;
import org.openl.util.FileTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class JavaWrapperAntTask extends Task {

    private static final String GOAL_MAKE_WRAPPER = "make wrapper", GOAL_UPDATE_PROPERTIES = "update properties",
            GOAL_MAKE_WEBINF = "make WEB-INF", GOAL_ALL = "all";

    private static String resName = "__res";

    private IOpenClass openClass;

    private String goal = GOAL_ALL;

    private String web_inf_path;

    private String web_inf_exclude = ".*apache.ant.*|.*apache.tomcat.*|.*javacc.*";

    private String web_inf_include = "";

    private String vocabularyClass;
    private String classpathExclude = ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*";

    private String projectHome = ".";

    private boolean ignoreNonJavaTypes = false;

    private String ignoreFields;

    private String ignoreMethods;

    private String userClassPath;

    private String userHome = ".";
    private String deplUserHome;

    private String srcFile, deplSrcFile;

    private String srcModuleClass;
    private String openlName;
    private String targetSrcDir;

    private String targetClass;

    private String displayName;

    private String[] methods;

    private String[] fields;

    private String s_package;

    private String s_class;

    private String extendsClass = null;

    /*
     * Full or relative path to directory where properties will be saved
     */
    private String classpathPropertiesOutputDir = ".";

    private String implementsInterfaces = OpenLWrapper.class.getName();

    private StringBuffer initBuf = new StringBuffer(1000);

    private String rulesFolder = "rules";

    private void addClassDeclaration(StringBuffer buf) {
        buf.append("public class " + s_class);
        if (extendsClass != null) {
            buf.append(" extends ").append(extendsClass);
        }
        if (implementsInterfaces != null) {
            buf.append(" implements ").append(implementsInterfaces);
        }
        buf.append("\n{\n");
    }

    private void addComment(StringBuffer buf) {
        buf
                .append("/*\n"
                        + " * This class has been generated. Do not change it, if you need to modify functionality - subclass it\n"
                        + " */\n\n"

                );
    }

    private void addEnvVariable(StringBuffer buf) {
        // declaration
        buf.append("  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){\n"
                        + "    @Override\n"
                        + "    protected org.openl.vm.IRuntimeEnv initialValue() {\n"
                        + "      return new org.openl.vm.SimpleVM().getRuntimeEnv();\n" 
                        + "    }\n" 
                        + "  };\n\n");
        // getter and setter
        buf.append("  public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {\n"
                    + "    return __env.get();\n"
                    + "  }\n\n" 
                    + "  public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv environment) {\n"
                    + "    __env.set(environment);\n" + "  }\n\n");
    }

    /**
     * @param field
     * @param buf
     */
    private void addFieldAccessor(IOpenField field, StringBuffer buf) {
        // public int getAbc()
        // {
        //
        // Object __res = abc_Field.get(__instance, __env.get());
        //
        // return ((Integer) __res).intValue();
        // }

        IOpenClass type = field.getType();

        String className = getClassName(type.getInstanceClass());

        buf.append("\n  public ").append(className).append(" get").append(fieldMethodPart(field)).append("()").append(
                "\n  {\n").append("   Object ").append(resName).append(" = ").append(getFieldFieldName(field)).append(
                ".get(__instance, __env.get());\n").append("   return ").append(
                castAndUnwrap(type.getInstanceClass(), resName)).append(";\n").append("  }\n\n");

    }

    private void addFieldField(IOpenField field, StringBuffer buf) {
        buf.append("\n\n  static " + IOpenField.class.getName() + " " + getFieldFieldName(field) + ";\n");
    }

    private void addFieldFieldInitializer(IOpenField field) {
        // abc_Field = __class.getField("abc");

        initBuf.append("    " + getFieldFieldName(field) + " = __class.getField(\"" + field.getName() + "\");\n");

    }

    private void addFieldModifier(IOpenField field, StringBuffer buf) {
        // public void setAbc(int x)
        // {
        //
        // abc_Field.set(__instance, new Integer(x) , __env.get());
        // }
        //
        String varname = "__var";
        IOpenClass type = field.getType();

        String className = getClassName(type.getInstanceClass());

        buf.append("\n  public void set").append(fieldMethodPart(field)).append("(").append(className).append(' ')
                .append(varname).append(")").append("\n  {\n").append("   ").append(getFieldFieldName(field)).append(
                        ".set(__instance, ").append(wrapIfPrimitive(varname, type.getInstanceClass())).append(
                        ", __env.get());\n").append("  }\n\n");

    }

    /**
     * @param buf
     * @param string
     */
    private void addImport(StringBuffer buf, String str) {
        buf.append("import ").append(str).append(";\n");

    }

    /**
     * @param buf
     */
    private void addInitMethod(StringBuffer buf) {

        String initStart =

        "  static boolean __initialized = false;\n\n"
                + "  static public void reset(){__initialized = false;}\n\n"

                + "public Object getInstance(){return __instance;}\n\n"

                + "public IOpenClass getOpenClass(){return __class;}\n\n"

                + "public org.openl.CompiledOpenClass getCompiledOpenClass(){return __compiledClass;}\n\n"

                + "public synchronized void  reload(){reset();__init();__instance = __class.newInstance(__env.get());}\n\n"

                + "  static synchronized protected void __init()\n"
                + "  {\n"
                + "    if (__initialized)\n"
                + "      return;\n\n"
                +

                "    IUserContext ucxt = UserContext.makeOrLoadContext(Thread.currentThread().getContextClassLoader(), __userHome);\n"
                + "    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , __src, __srcModuleClass);\n"

                + "    __compiledClass = wrapper.getCompiledClass();\n"
                + "    __class = wrapper.getOpenClassWithErrors();\n"

                + "   // __env.set(wrapper.getEnv());\n\n";

        buf.append(initStart).append(initBuf.toString()).append("\n    __initialized=true;\n  }\n");
    }

    private void addMethodAccessor(IOpenMethod method, StringBuffer buf, boolean isStatic) {
        addMethodSignature(method, buf, isStatic);
        addMethodBody(method, buf, isStatic);
    }

    private void addMethodBody(IOpenMethod method, StringBuffer buf, boolean isStatic) {
        buf.append("  {\n");

        IOpenClass[] ptypes = method.getSignature().getParameterTypes();

        // Object[] __params = new Object[2];

        buf.append("    Object[] __params = new Object[").append(ptypes.length).append("];");

        // params[0] = new Integer[p1];
        // params[1] = p2;

        for (int i = 0; i < ptypes.length; i++) {
            buf.append("\n    __params[").append(i).append("] = ").append(parameterToObject(method, i)).append(';');
        }

        // try
        // {

        //
        // Object res = XYZ_Method.invoke(instance, params, env);

        buf.append("\n    try\n    {\n");

        // Object instance = new Demo1().__instance;
        buf.append("    Object __myInstance = ");
        if (isStatic) {
            buf.append("new " + s_class + "().");
        }

        buf.append("__instance;\n");

        buf.append(returnMethodVar(method, resName)).append(getMethodFieldName(method)).append(
                ".invoke(__myInstance, __params, __env.get());");
        //
        // return ((Double)res).doubleValue();

        buf.append(returnMethodResult(method, resName));
        buf.append("  }\n" + "  catch(Throwable t)\n" + "  {\n"
                + "    Log.error(\"Java Wrapper execution error:\", t);\n"
                + "    throw RuntimeExceptionWrapper.wrap(t);\n" + "  }\n");

        buf.append("\n  }\n");
    }

    private void addMethodField(IOpenMethod method, StringBuffer buf) {
        buf.append("\n\n  static " + IOpenMethod.class.getName() + " " + getMethodFieldName(method) + ";\n");
    }

    /**
     * @param method
     */
    private void addMethodFieldInitializer(IOpenMethod method) {
        // XYZ_Method = __class.getMatchingMethod("XYZ", new IOpenClass[] {
        // JavaOpenClass.getOpenClass(int.class),
        // JavaOpenClass.getOpenClass(String.class) });

        initBuf.append("    " + getMethodFieldName(method) + " = __class.getMatchingMethod(\"" + method.getName()
                + "\", new IOpenClass[] {\n");

        IOpenClass[] params = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                initBuf.append(",\n");
            }

            IOpenClass param = params[i];
			if (param instanceof ModuleOpenClass) {
				initBuf.append("((XlsModuleOpenClass)__class)")
					   .append(String.format(".findType(ISyntaxConstants.THIS_NAMESPACE, \"%s\")", param.getName()));
			} else {
				// JavaOpenClass.getOpenClass(int.class),
				initBuf.append("      JavaOpenClass.getOpenClass(")
					   .append(getClassName(params[i].getInstanceClass()))
					   .append(".class)");
			}
        }

        initBuf.append("});\n");
    }

    private void addMethodSignature(IOpenMethod method, StringBuffer buf, boolean isStatic) {
        buf.append("  public ");
        if (isStatic) {
            buf.append("static ");
        }
        buf.append(getMethodType(method)).append(' ');
        buf.append(getMethodName(method));
        buf.append('(');
        IOpenClass[] ptypes = method.getSignature().getParameterTypes();
        for (int i = 0; i < ptypes.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(getOpenClassType(ptypes[i])).append(' ').append(
                    getParamName(method.getSignature().getParameterName(i), i));
        }
        buf.append(')');
    }

    private void addPackage(StringBuffer buf) {
        if (s_package != null) {
            buf.append("package " + s_package + ";\n\n");
        }
    }

    private int calcMethods(IOpenClass ioc) {
        int cnt = 0;

        for (Iterator<IOpenMethod> iter = ioc.methods(); iter.hasNext();) {
            IOpenMethod method = iter.next();
            if (!isMethodGenerated(method)) {
                continue;
            }
            ++cnt;
        }

        return cnt;
    }

    /**
     * @param instanceClass
     * @param resVarName
     * @return
     */
    private String castAndUnwrap(Class<?> instanceClass, String resVarName) {
        if (instanceClass == Object.class) {
            return resVarName;
        }

        if (instanceClass.isPrimitive()) {
            return unwrapIfPrimitive(instanceClass, resVarName);
        }

        return "(" + getClassName(instanceClass) + ")" + resVarName;
    }

    @Override
    public void execute() throws BuildException {
        try {
            run();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    /**
     * @param field
     * @return
     */
    private String fieldMethodPart(IOpenField field) {
        String name = field.getName();
        return StringTool.capitalize(name);

    }

    /**
     * @return
     * @throws IOException
     */
    private String filterClassPath() throws IOException {
        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        StringBuffer buf = new StringBuffer(300);

        for (int i = 0; i < tokens.length; i++) {
            // System.out.println(tokens[i]);
            if (tokens[i].matches(classpathExclude)) {
                continue;
            }
            File f = FileTool.buildRelativePath(new File(projectHome), new File(tokens[i]));
            String relativePath = f.getPath().replace('\\', '/');
            buf.append(relativePath).append(File.pathSeparator);
        }
        return buf.toString();
    }

    private String generateJavaClass(IOpenClass ioc) {
        StringBuffer buf = new StringBuffer(10000);

        parseClassName();

        addComment(buf);

        addPackage(buf);

        int cnt = calcMethods(ioc);
        if (cnt != 0) {
            addImport(buf, "org.openl.util.Log");
            addImport(buf, "org.openl.util.RuntimeExceptionWrapper");
            addImport(buf, "org.openl.types.java.JavaOpenClass");
        }

        addImport(buf, "org.openl.types.IOpenClass");
        addImport(buf, "org.openl.conf.IUserContext");
        addImport(buf, "org.openl.conf.UserContext");

        addImport(buf, "org.openl.impl.OpenClassJavaWrapper");
        addImport(buf, "org.openl.rules.lang.xls.binding.XlsModuleOpenClass");
        addImport(buf, "org.openl.syntax.impl.ISyntaxConstants");

        addClassDeclaration(buf);

        buf.append("  Object __instance;\n\n");

        buf.append("  public static org.openl.types.IOpenClass __class;\n\n");

        buf.append("  public static org.openl.CompiledOpenClass __compiledClass;\n\n");

        buf.append("  public static String __openlName = \"" + StringEscapeUtils.escapeJava(openlName) + "\";\n\n");

        buf.append("  public static String __src = \""
                + StringEscapeUtils.escapeJava(deplSrcFile == null ? srcFile : deplSrcFile) + "\";\n\n");

        buf.append("  public static String __srcModuleClass = "
                + (srcModuleClass == null ? null : "\"" + StringEscapeUtils.escapeJava(srcModuleClass) + "\"")
                + ";\n\n");

        buf.append("  public static String __folder = \"" + StringEscapeUtils.escapeJava(rulesFolder) + "\";\n\n");

        buf.append("  public static String __project = \"" + StringEscapeUtils.escapeJava(getRulesProject())
                + "\";\n\n");

        buf.append("  public static String __userHome = \""
                + StringEscapeUtils.escapeJava(deplUserHome == null ? userHome : deplUserHome) + "\";\n\n");

        addEnvVariable(buf);

        buf.append("  public " + s_class + "(){\n" + "    this(false);\n" + "  }\n\n");

        buf.append("  public " + s_class + "(boolean ignoreErrors){\n" + "    __init();\n"
                + "    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();\n"
                + "    __instance = __class.newInstance(__env.get());\n" + "  }\n\n");

        buf.append("");

        for (Iterator<IOpenField> iter = ioc.fields(); iter.hasNext();) {
            IOpenField field = iter.next();
            if (!isFieldGenerated(field)) {
                continue;
            }
            addFieldFieldInitializer(field);
            addFieldField(field, buf);
            addFieldAccessor(field, buf);
            addFieldModifier(field, buf);
        }

        for (Iterator<IOpenMethod> iter = ioc.methods(); iter.hasNext();) {
            IOpenMethod method = iter.next();
            if (!isMethodGenerated(method)) {
                continue;
            }
            addMethodFieldInitializer(method);
            addMethodField(method, buf);
            addMethodAccessor(method, buf, isStatic(method));
        }

        addInitMethod(buf);

        buf.append("}");

        return buf.toString();
    }

    /**
     * @param instanceClass
     * @return
     */
    private String getClassName(Class<?> instanceClass) {
        StringBuffer buf = new StringBuffer(30);
        while (instanceClass.isArray()) {
            buf.append("[]");
            instanceClass = instanceClass.getComponentType();
        }

        buf.insert(0, getScalarClassName(instanceClass));
        return buf.toString();
    }

; String getClasspathExclude() {
        return classpathExclude;
    }

    /*
     * Get full or relative path to directory where classpath properties will be
     * save
     */
    public String getClasspathPropertiesOutputDir() {
        return classpathPropertiesOutputDir;
    }

    public String getDeplSrcFile() {
        return deplSrcFile;
    }

    public String getDeplUserHome() {
        return deplUserHome;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtendsClass() {
        return extendsClass;
    }

    public String getFieldFieldName(IOpenField field) {
        return field.getName() + "_Field";
    }

    public String[] getFields() {
        return fields;
    }

    public String getGoal() {
        return goal;
    }

    public String getIgnoreFields() {
        return ignoreFields;
    }

    public String getIgnoreMethods() {
        return ignoreMethods;
    }

    public String getImplementsInterfaces() {
        return implementsInterfaces;
    }

    /**
     * @param method
     * @return
     */
    public String getMethodFieldName(IOpenMethod method) {
        return getMethodName(method) + "_Method";
    }

    /**
     * @param method
     * @return
     */
    public String getMethodName(IOpenMethod method) {
        return method.getName();
    }

    public String[] getMethods() {
        return methods;
    }

    public String getMethodType(IOpenMethod method) {
        return getOpenClassType(method.getType());
    }

    /**
     * @param type
     * @return
     */
    public String getOpenClassType(IOpenClass type) {
        return getClassName(type.getInstanceClass());
    }

    public String getOpenlName() {
        return openlName;
    }

    private String getOutputFileName() {
        String file = targetSrcDir + "/" + targetClass.replace('.', '/') + ".java";
        return file;
    }

    /**
     * @param parameterName
     * @return
     */
    private String getParamName(String parameterName, int i) {
        return parameterName == null ? "arg" + i : parameterName;
    }

    public String getRulesFolder() {
        return rulesFolder;
    }

    private String getRulesProject() {
        try {
            String file = new File(".").getCanonicalFile().toString();
            String[] tokens = StringTool.tokenize(file, "/\\");

            return tokens[tokens.length - 1];

        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

    }

    public String getS_class() {
        return s_class;
    }

    public String getS_package() {
        return s_package;
    }

    public String getScalarClassName(Class<?> instanceClass) {
        return instanceClass.getName();
    }

    public String getSrcFile() {
        return srcFile;
    }

    public String getSrcModuleClass() {
        return srcModuleClass;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetSrcDir() {
        return targetSrcDir;
    }

    public String getUserClassPath() {
        return userClassPath;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getVocabularyClass() {
        return vocabularyClass;
    }

    public String getWeb_inf_exclude() {
        return web_inf_exclude;
    }

    public String getWeb_inf_include() {
        return web_inf_include;
    }

    public String getWeb_inf_path() {
        return web_inf_path;
    }

    private boolean isFieldGenerated(IOpenField field) {
        if (fields != null && !ArrayTool.contains(fields, field.getName())) {
            return false;
        }

        IOpenClass type = field.getType();
        if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
            return false;
        }

        return true;
    }

    public boolean isIgnoreNonJavaTypes() {
        return ignoreNonJavaTypes;
    }

    private boolean isMethodGenerated(IOpenMethod method) {

        // TODO fix a) provide isConstructor() in OpenMethod b) provide better
        // name for XLS modules
        if (openClass.getName().equals(method.getName())) {
            return false;
        }

        if ("getOpenClass".equals(method.getName())) {
            return false;
        }

        if (methods != null && !ArrayTool.contains(methods, method.getName())) {
            return false;
        }

        IOpenClass type = method.getType();
        if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
            return false;
        }

        IOpenClass[] params = method.getSignature().getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            type = params[i];
            if (ignoreNonJavaTypes && !(type instanceof JavaOpenClass)) {
                return false;
            }

        }
        return true;
    }

    /**
     * @param method
     * @return
     */
    private boolean isStatic(IOpenMethod method) {
        return method.getName().equals("main") && method.getSignature().getParameterTypes().length == 1
                && method.getSignature().getParameterTypes()[0].getInstanceClass().equals(String[].class);
    }

    private IOpenClass makeOpenClass() throws Exception {

        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), userHome);
        if (userClassPath != null) {
            ClassLoader cl = ClassLoaderFactory
                    .createClassLoader(userClassPath, this.getClass().getClassLoader(), ucxt);

            ucxt = new UserContext(cl, userHome);
            Thread.currentThread().setContextClassLoader(cl);
        }
        OpenClassJavaWrapper jwrapper = OpenClassJavaWrapper.createWrapper(openlName, ucxt, srcFile);
        return jwrapper.getOpenClass();

    }

    private void makeWebInfPath() {
        String targetFolder = web_inf_path;

        String classes_target = targetFolder + "/classes";
        String lib_target = targetFolder + "/lib";

        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        System.out.println("Making WEB-INF...");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches(web_inf_exclude) && !tokens[i].matches(web_inf_include)) {
                continue;
            }
            System.out.println(tokens[i]);

            File f = new File(tokens[i]);

            if (f.isDirectory()) {
                Copy cpy = new Copy();
                cpy.setProject(getProject());
                cpy.setTodir(new File(classes_target));
                FileSet fset = new FileSet();
                fset.setDir(f);
                cpy.addFileset(fset);
                cpy.setVerbose(true);
                cpy.execute();
            } else {
                Copy cpy = new Copy();
                cpy.setProject(getProject());
                cpy.setTodir(new File(lib_target));
                cpy.setFile(f);
                cpy.setVerbose(true);
                cpy.execute();
            }
        }

    }

    /**
     * @param method
     * @param i
     * @return
     */
    private String parameterToObject(IOpenMethod method, int i) {
        IOpenClass type = method.getSignature().getParameterTypes()[i];
        String name = getParamName(method.getSignature().getParameterName(i), i);

        Class<?> instanceClass = type.getInstanceClass();
        if (instanceClass.isPrimitive()) {
            return wrapIfPrimitive(name, instanceClass);
        }

        return name;
    }

    /**
     *
     */
    private void parseClassName() {
        int idx = targetClass.lastIndexOf('.');
        if (idx < 0) {
            s_class = targetClass;
        } else {
            s_package = targetClass.substring(0, idx);
            s_class = targetClass.substring(idx + 1, targetClass.length());
        }
    }

    /**
     * @param method
     * @param string
     * @return
     */
    private String returnMethodResult(IOpenMethod method, String resVarName) {
        IOpenClass type = method.getType();

        Class<?> instanceClass = type.getInstanceClass();
        if (instanceClass == void.class) {
            return "";
        }

        return "\n   return " + castAndUnwrap(instanceClass, resVarName) + ";";
    }

    private String returnMethodVar(IOpenMethod method, String resVarName) {

        IOpenClass type = method.getType();

        Class<?> instanceClass = type.getInstanceClass();
        if (instanceClass == void.class) {
            return "    ";
        }
        return "    Object " + resVarName + " = ";
    }

    private void run() throws Exception {
        if (ignoreFields != null) {
            fields = StringTool.tokenize(ignoreFields, ", ");
        }

        if (ignoreMethods != null) {
            methods = StringTool.tokenize(ignoreMethods, ", ");
        }

        if (goal.equals(GOAL_ALL) || goal.contains(GOAL_UPDATE_PROPERTIES)) {
            saveProjectProperties();
        }

        if (goal.equals(GOAL_ALL) || goal.contains(GOAL_MAKE_WRAPPER)) {
            openClass = makeOpenClass();
            String content = generateJavaClass(openClass);
            FileWriter fw = null;
            try {
                String fileName = getOutputFileName();
                new File(fileName).getParentFile().mkdirs();
                fw = new FileWriter(fileName);
                fw.write(content);
            } finally {
                if (fw != null) {
                    fw.close();
                }
            }
        }

        if (goal.contains(GOAL_MAKE_WEBINF)) {
            if (web_inf_path == null) {
                throw new RuntimeException("web_inf_path is not set");
            }
        }

        if (goal.equals(GOAL_ALL) || goal.contains(GOAL_MAKE_WRAPPER)) {
            if (web_inf_path == null) {
                return;
            }

            makeWebInfPath();
        }

    }

    /**
     * @throws IOException
     *
     */
    @SuppressWarnings("unchecked")
    private void saveProjectProperties() throws IOException {
        Properties p = new Properties();
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_PROPERTY, filterClassPath());

        if (displayName != null) {
            p.put(targetClass + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, displayName);
        }

        if (vocabularyClass != null) {
            try {
                Class c = Class.forName(vocabularyClass);
                Object instance = c.newInstance();
                @SuppressWarnings("unused")
                IVocabulary vocabulary = (IVocabulary) instance;
            } catch (Throwable t) {
                System.err.println("Error occured while trying instantiate vocabulary class:" + vocabularyClass);
                t.printStackTrace();
            }

            p.put(targetClass + OpenLProjectPropertiesLoader.VOCABULARY_CLASS_SUFFIX, vocabularyClass);
        }

        new OpenLProjectPropertiesLoader().saveProperties(classpathPropertiesOutputDir, p, false);
    }

    public void setClasspathExclude(String classpathExclude) {
        this.classpathExclude = classpathExclude;
    }

    /*
     * Set full or relative path to directory where classpath properties will be
     * save
     */
    public void setClasspathPropertiesOutputDir(String classpathPropertiesOutputDir) {
        this.classpathPropertiesOutputDir = classpathPropertiesOutputDir;
    }

    public void setDeplSrcFile(String deplSrcFile) {
        this.deplSrcFile = deplSrcFile;
    }

    public void setDeplUserHome(String deplUserHome) {
        this.deplUserHome = deplUserHome;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setExtendsClass(String extendsClass) {
        this.extendsClass = extendsClass;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public void setIgnoreFields(String ignoreFields) {
        this.ignoreFields = ignoreFields;
    }

    public void setIgnoreMethods(String ignoreMethods) {
        this.ignoreMethods = ignoreMethods;
    }

    public void setIgnoreNonJavaTypes(boolean ignoreNonJavaTypes) {
        this.ignoreNonJavaTypes = ignoreNonJavaTypes;
    }

    public void setImplementsInterfaces(String implementsInterfaces) {
        this.implementsInterfaces = this.implementsInterfaces + "," + implementsInterfaces;
    }

    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    public void setOpenlName(String openlName) {
        this.openlName = openlName;
    }

    public void setRulesFolder(String rulesFolder) {
        this.rulesFolder = rulesFolder;
    }

    public void setS_class(String s_class) {
        this.s_class = s_class;
    }

    public void setS_package(String s_package) {
        this.s_package = s_package;
    }

    public void setSrcFile(String srcFile) {
        this.srcFile = srcFile;
    }

    public void setSrcModuleClass(String srcModuleClass) {
        this.srcModuleClass = srcModuleClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public void setTargetSrcDir(String targetSrcDir) {
        this.targetSrcDir = targetSrcDir;
    }

    public void setUserClassPath(String userClassPath) {
        this.userClassPath = userClassPath;
    }

    public void setUserHome(String userHome) {
        this.userHome = userHome;
    }

    public void setVocabularyClass(String vocabularyClass) {
        this.vocabularyClass = vocabularyClass;
    }

    public void setWeb_inf_exclude(String web_inf_exclude) {
        this.web_inf_exclude = web_inf_exclude;
    }

    public void setWeb_inf_include(String web_inf_include) {
        this.web_inf_include = web_inf_include;
    }

    public void setWeb_inf_path(String web_inf_path) {
        this.web_inf_path = web_inf_path;
    }

    /**
     * @param instanceClass
     * @param resName
     * @return
     */
    private String unwrapIfPrimitive(Class<?> instanceClass, String name) {
        if (instanceClass == int.class) {
            return "((Integer)" + name + ").intValue()";
        }
        if (instanceClass == double.class) {
            return "((Double)" + name + ").doubleValue()";
        }
        if (instanceClass == boolean.class) {
            return "((Boolean)" + name + ").booleanValue()";
        }
        if (instanceClass == char.class) {
            return "((Character)" + name + ").charValue()";
        }
        if (instanceClass == long.class) {
            return "((Long)" + name + ").longValue()";
        }
        if (instanceClass == short.class) {
            return "((Short)" + name + ").shortValue()";
        }
        if (instanceClass == float.class) {
            return "((Float)" + name + ").floatValue()";
        }
        return name;
    }

    /**
     * @param name
     * @param instanceClass
     * @return
     */
    private String wrapIfPrimitive(String name, Class<?> instanceClass) {
        if (instanceClass == int.class) {
            return "new Integer(" + name + ")";
        }
        if (instanceClass == double.class) {
            return "new Double(" + name + ")";
        }
        if (instanceClass == boolean.class) {
            return "new Boolean(" + name + ")";
        }
        if (instanceClass == char.class) {
            return "new Character(" + name + ")";
        }
        if (instanceClass == long.class) {
            return "new Long(" + name + ")";
        }
        if (instanceClass == short.class) {
            return "new Short(" + name + ")";
        }
        if (instanceClass == float.class) {
            return "new Float(" + name + ")";
        }

        return name;
    }

}
