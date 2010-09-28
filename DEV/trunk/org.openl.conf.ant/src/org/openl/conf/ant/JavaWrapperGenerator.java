package org.openl.conf.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.base.INamedThing;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.ArrayTool;
import org.openl.util.ISelector;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;
import org.openl.util.generation.JavaClassGeneratorHelper;

public class JavaWrapperGenerator {
    
    private String targetClass;
    private String extendsClass = null;
    private String[] implementsInterfaces;
    private String openlName;
    private String deplSrcFile;
    private String srcFile;
    private String srcModuleClass;
    private String userHome = ".";
    private String deplUserHome;
    private String rulesFolder;
    private String[] fields;
    private String[] methods;    
    private boolean ignoreNonJavaTypes;
    
    private IOpenClass moduleOpenClass;
    
    private String s_class;
    private String s_package;
    private static String resName = "__res";
    private StringBuffer initBuf = new StringBuffer(1000);
    private List<String> defaultImports;
    private List<String> methodImports;
    
    
    public JavaWrapperGenerator(String targetClass, String extendsClass, String[] implementsInterfaces, 
            String openlName, String deplSrcFile, String srcFile, String srcModuleClass, String userHome, String deplUserHome,
            String rulesFolder, String[] fields, String[] methods, boolean ignoreNonJavaTypes) {
        this.targetClass = targetClass;
        this.extendsClass = extendsClass;
        if (implementsInterfaces != null) {
            this.implementsInterfaces = implementsInterfaces.clone();
        }        
        this.openlName = openlName;
        this.deplSrcFile = deplSrcFile;
        this.srcFile = srcFile;
        this.srcModuleClass = srcModuleClass;
        this.userHome = userHome;
        this.deplUserHome = deplUserHome;
        this.rulesFolder = rulesFolder;
        if (methods != null) {
            this.methods = methods.clone();
        }
        if (fields != null) {
            this.fields = fields.clone();
        }
        this.ignoreNonJavaTypes = ignoreNonJavaTypes;
        
        initImports();
    }
    
    private void initImports() {
        defaultImports = new ArrayList<String>();
        defaultImports.add("java.util.Map");
        defaultImports.add("org.openl.types.IOpenClass");
        defaultImports.add("org.openl.conf.IUserContext");
        defaultImports.add("org.openl.conf.UserContext");
        defaultImports.add("org.openl.impl.OpenClassJavaWrapper");
        defaultImports.add("org.openl.source.impl.FileSourceCodeModule");
        
        methodImports = new ArrayList<String>();
        methodImports.add("org.openl.util.Log");
        methodImports.add("org.openl.util.RuntimeExceptionWrapper");        
        methodImports.add("org.openl.types.java.OpenClassHelper");
    }

    public String generateJavaClass(IOpenClass moduleOpenClass) {
        this.moduleOpenClass = moduleOpenClass;
        StringBuffer buf = new StringBuffer(10000);

        parseClassName();

        addComment(buf);

        addPackage(buf);

        addImports(moduleOpenClass, buf);

        addClassDeclaration(buf);

        addInnerFields(buf);

        addEnvVariable(buf);
        
        addRuntimeContextProvider(buf);

        addRuntimeContextConsumer(buf);
        
        addConstructorWithParameter(buf);

        addFieldMethods(moduleOpenClass, buf);

        addMethods(moduleOpenClass, buf);

        addInitMethod(buf);

        buf.append("}");

        return buf.toString();
    }
    
    private void parseClassName() {
        int idx = targetClass.lastIndexOf('.');
        if (idx < 0) {
            s_class = targetClass;
        } else {
            s_package = targetClass.substring(0, idx);
            s_class = targetClass.substring(idx + 1, targetClass.length());
        }
    }
    
    private void addComment(StringBuffer buf) {
        buf.append(JavaClassGeneratorHelper.getCommentText("This class has been generated. Do not change it, if you need to modify functionality - subclass it"));
    }

    private void addEnvVariable(StringBuffer buf) {
        // declaration
        buf.append("  private ThreadLocal<org.openl.vm.IRuntimeEnv> __env = new ThreadLocal<org.openl.vm.IRuntimeEnv>(){\n")
           .append("    @Override\n")
           .append("    protected org.openl.vm.IRuntimeEnv initialValue() {\n")
           .append("      org.openl.vm.IRuntimeEnv environment = new org.openl.vm.SimpleVM().getRuntimeEnv();\n")
           .append("      environment.setContext(new org.openl.rules.context.DefaultRulesRuntimeContext());\n")
           .append("      return environment;\n")
           .append("    }\n")
           .append("  };\n\n");
        // getter and setter
        buf.append("  public org.openl.vm.IRuntimeEnv getRuntimeEnvironment() {\n" + "    return __env.get();\n" + "  }\n\n" + "  public void setRuntimeEnvironment(org.openl.vm.IRuntimeEnv environment) {\n" + "    __env.set(environment);\n" + "  }\n\n");
    }

    private void addRuntimeContextProvider(StringBuffer buf) {
        buf.append("  public org.openl.rules.context.IRulesRuntimeContext getRuntimeContext() {\n");
        buf.append("    return (org.openl.rules.context.IRulesRuntimeContext)getRuntimeEnvironment().getContext();\n");
        buf.append("  }\n\n");
    }

    private void addRuntimeContextConsumer(StringBuffer buf) {
        buf.append("  public void setRuntimeContext(org.openl.rules.context.IRulesRuntimeContext context) {\n");
        buf.append("    getRuntimeEnvironment().setContext(context);\n");
        buf.append("  }\n\n");
    }

    private void addFieldAccessor(IOpenField field, StringBuffer buf) {

        IOpenClass type = field.getType();

        String className = getClassName(type.getInstanceClass());

        buf.append("\n  public ")
            .append(className)
            .append(" get")
            .append(fieldMethodPart(field))
            .append("()")
            .append("\n  {\n")
            .append("   Object ")
            .append(resName)
            .append(" = ")
            .append(getFieldFieldName(field))
            .append(".get(__instance, __env.get());\n")
            .append("   return ")
            .append(castAndUnwrap(type.getInstanceClass(), resName))
            .append(";\n")
            .append("  }\n\n");

    }
    
    private void addClassDeclaration(StringBuffer buf) {        
        String classDeclaration = null;
        if (extendsClass != null) {
            classDeclaration = JavaClassGeneratorHelper.addExtendingClassDeclaration(s_class, extendsClass);
        } else {
            classDeclaration = JavaClassGeneratorHelper.getSimplePublicClassDeclaration(s_class);
        }
        if (implementsInterfaces != null) {            
            buf.append(JavaClassGeneratorHelper.addImplementingInterfToClassDeclaration(classDeclaration, implementsInterfaces));
        }
        buf.append("\n{\n");
    }
    
    private void addFieldMethods(IOpenClass moduleOpenClass, StringBuffer buf) {
        for (IOpenField field : moduleOpenClass.getFields().values()) {            
            if (!isFieldGenerated(field)) {
                continue;
            }
            addFieldFieldInitializer(field);
            addFieldField(field, buf);
            addFieldAccessor(field, buf);
            addFieldModifier(field, buf);
        }
    }
    
    private void addMethods(IOpenClass moduleOpenClass, StringBuffer buf) {
        for (IOpenMethod method : moduleOpenClass.getMethods()) {            
            if (!isMethodGenerated(method)) {
                continue;
            }
            addMethodFieldInitializer(method);
            addMethodField(method, buf);
            addMethodAccessor(method, buf, isStatic(method));
        }
    }
    
    private void addInnerFields(StringBuffer buf) {        
        buf.append(JavaClassGeneratorHelper.getDefaultFieldDeclaration(Object.class.getName(), "__instance"));

        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldDeclaration(IOpenClass.class.getName(), "__class"));

        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldDeclaration(CompiledOpenClass.class.getName(), "__compiledClass"));

        buf.append("  private static Map<String, Object> __externalParams;\n");

        String initializationValue = String.format("\"%s\"", StringEscapeUtils.escapeJava(openlName));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__openlName", 
            initializationValue));

        String initializationValueSrc = String.format("\"%s\"", StringEscapeUtils.escapeJava(deplSrcFile == null ? srcFile : deplSrcFile));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__src", 
            initializationValueSrc));

        String initValue = srcModuleClass == null ? null : String.format("\"%s\"", StringEscapeUtils.escapeJava(srcModuleClass));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__srcModuleClass", 
            initValue));

        String initializationValueRulesFolder = String.format("\"%s\"", StringEscapeUtils.escapeJava(rulesFolder));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__folder", 
            initializationValueRulesFolder));

        String initializationValueProject = String.format("\"%s\"", StringEscapeUtils.escapeJava(getRulesProject()));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__project", 
            initializationValueProject));

        String initializationValueUserHome = String.format("\"%s\"", StringEscapeUtils.escapeJava(deplUserHome == null ? userHome : deplUserHome));
        buf.append(JavaClassGeneratorHelper.getStaticPublicFieldInitialization(String.class.getName(), "__userHome", 
            initializationValueUserHome));
    }

    private void addImports(IOpenClass moduleOpenClass, StringBuffer buf) {
        int methodsNum = calcMethods(moduleOpenClass);
        if (methodsNum != 0) {
            for (String methodImport : methodImports) {
                addImport(buf, methodImport);
            }
        }
        
        for (String defaultImport : defaultImports) {
            addImport(buf, defaultImport);
        }
    }
    
    private void addConstructorWithParameter(StringBuffer buf) {
        buf.append("  public ")
            .append(s_class)
            .append("(){\n")
            .append("    this(false, null);\n")
            .append("  }\n\n");
        
        buf.append("  public ")
            .append(s_class)
            .append("(Map<String, Object> params){\n")
            .append("    this(false, params);\n")
            .append("  }\n\n");

        buf.append("  public ")
            .append(s_class)
            .append("(boolean ignoreErrors, Map<String, Object> params){\n")
            .append("    __externalParams = params;\n")
            .append("    __init();\n")
            .append("    if (!ignoreErrors) __compiledClass.throwErrorExceptionsIfAny();\n")
            .append("    __instance = __class.newInstance(__env.get());\n")
            .append("  }\n\n");

        buf.append("");
    }
    
    private void addPackage(StringBuffer buf) {
        buf.append(JavaClassGeneratorHelper.getPackageText(s_package));
    }

    private int calcMethods(IOpenClass ioc) {
        int cnt = 0;

        for (IOpenMethod method : ioc.getMethods()) {            
            if (!isMethodGenerated(method)) {
                continue;
            }
            ++cnt;
        }

        return cnt;
    }
    
    private void addMethodField(IOpenMethod method, StringBuffer buf) {
        buf.append("\n\n  static " + IOpenMethod.class.getName() + " " + getMethodFieldName(method) + ";\n");
    }

    private void addMethodFieldInitializer(IOpenMethod method) {
        // XYZ_Method = __class.getMatchingMethod("XYZ", new IOpenClass[] {
        // JavaOpenClass.getOpenClass(int.class),
        // JavaOpenClass.getOpenClass(String.class) });

        initBuf.append("    " + getMethodFieldName(method) + " = __class.getMatchingMethod(\"" + method.getName() + "\", new IOpenClass[] {\n");

        IOpenClass[] params = method.getSignature().getParameterTypes();

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                initBuf.append(",\n");
            }

//            IOpenClass param = params[i];
            initBuf.append("      OpenClassHelper.getOpenClass(__class, ")
            .append(getClassName(params[i].getInstanceClass()))
            .append(".class)");
            
//            if (param instanceof DatatypeOpenClass) {
//                initBuf.append("((org.openl.rules.lang.xls.binding.XlsModuleOpenClass)__class)")
//                    .append(String.format(".findType(org.openl.syntax.impl.ISyntaxConstants.THIS_NAMESPACE, \"%s\")",
//                        param.getName()));
//            } else {
//                // JavaOpenClass.getOpenClass(int.class),
//                initBuf.append("      JavaOpenClass.getOpenClass(")
//                    .append(getClassName(params[i].getInstanceClass()))
//                    .append(".class)");
//            }
        }

        initBuf.append("});\n");
    }
    
    private void addImport(StringBuffer buf, String str) {
        buf.append(JavaClassGeneratorHelper.getImportText(str));
    }

    private void addInitMethod(StringBuffer buf) {

        String initStart =

        "  static boolean __initialized = false;\n\n" + "  static public void reset(){__initialized = false;}\n\n"

        + "public Object getInstance(){return __instance;}\n\n"

        + "public IOpenClass getOpenClass(){return __class;}\n\n"

        + "public org.openl.CompiledOpenClass getCompiledOpenClass(){return __compiledClass;}\n\n"

        + "public synchronized void  reload(){reset();__init();__instance = __class.newInstance(__env.get());}\n\n"

        + "  static synchronized protected void __init()\n" + "  {\n" + "    if (__initialized)\n" + "      return;\n\n" 
        
        + "    IUserContext ucxt = UserContext.makeOrLoadContext(Thread.currentThread().getContextClassLoader(), __userHome);\n" 
        
        + "    FileSourceCodeModule source = new FileSourceCodeModule(__src, null);\n"
        
        + "    source.setParams(__externalParams);\n"
        
        + "    OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper(__openlName, ucxt , source);\n"

        + "    __compiledClass = wrapper.getCompiledClass();\n" + "    __class = wrapper.getOpenClassWithErrors();\n"

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

        buf.append("    Object[] __params = new Object[").append(ptypes.length).append("];");

        for (int i = 0; i < ptypes.length; i++) {
            buf.append("\n    __params[").append(i).append("] = ").append(parameterToObject(method, i)).append(';');
        }

        buf.append("\n    try\n    {\n");

        // Object instance = new Demo1().__instance;
        buf.append("    Object __myInstance = ");
        if (isStatic) {
            buf.append("new " + s_class + "().");
        }

        buf.append("__instance;\n");

        buf.append(returnMethodVar(method, resName))
            .append(getMethodFieldName(method))
            .append(".invoke(__myInstance, __params, __env.get());");
        //
        // return ((Double)res).doubleValue();

        buf.append(returnMethodResult(method, resName));
        buf.append("  }\n" + "  catch(Throwable t)\n" + "  {\n" + "    Log.error(\"Java Wrapper execution error:\", t);\n" + "    throw RuntimeExceptionWrapper.wrap(t);\n" + "  }\n");

        buf.append("\n  }\n");
    }
    
    private void addFieldField(IOpenField field, StringBuffer buf) {
        buf.append("\n\n  static " + IOpenField.class.getName() + " " + getFieldFieldName(field) + ";\n");
    }

    private void addFieldFieldInitializer(IOpenField field) {
        initBuf.append("    " + getFieldFieldName(field) + " = __class.getField(\"" + field.getName() + "\");\n");
    }

    private void addFieldModifier(IOpenField field, StringBuffer buf) {
        String varname = "__var";
        IOpenClass type = field.getType();

        String className = getClassName(type.getInstanceClass());

        buf.append("\n  public void set")
            .append(fieldMethodPart(field))
            .append("(")
            .append(className)
            .append(' ')
            .append(varname)
            .append(")")
            .append("\n  {\n")
            .append("   ")
            .append(getFieldFieldName(field))
            .append(".set(__instance, ")
            .append(wrapIfPrimitive(varname, type.getInstanceClass()))
            .append(", __env.get());\n")
            .append("  }\n\n");
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
            buf.append(getOpenClassType(ptypes[i])).append(' ').append(getParamName(method.getSignature()
                .getParameterName(i), i));
        }
        buf.append(')');
    }
    
    private String parameterToObject(IOpenMethod method, int i) {
        IOpenClass type = method.getSignature().getParameterTypes()[i];
        String name = getParamName(method.getSignature().getParameterName(i), i);

        Class<?> instanceClass = type.getInstanceClass();
        if (instanceClass.isPrimitive()) {
            return wrapIfPrimitive(name, instanceClass);
        }

        return name;
    }

    

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
    
    private boolean isStatic(IOpenMethod method) {
        return method.getName().equals("main") && method.getSignature().getParameterTypes().length == 1 && method.getSignature()
            .getParameterTypes()[0].getInstanceClass().equals(String[].class);
    }
    
    private String castAndUnwrap(Class<?> instanceClass, String resVarName) {
        if (instanceClass == Object.class) {
            return resVarName;
        }

        if (instanceClass.isPrimitive()) {
            return unwrapIfPrimitive(instanceClass, resVarName);
        }

        return "(" + getClassName(instanceClass) + ")" + resVarName;
    }
    
    private String fieldMethodPart(IOpenField field) {
        String name = field.getName();
        return StringUtils.capitalize(name);

    }
    
    private String getFieldFieldName(IOpenField field) {
        return field.getName() + "_Field";
    }
    
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
    
    private boolean isMethodGenerated(IOpenMethod method) {

        // TODO fix a) provide isConstructor() in OpenMethod b) provide better
        // name for XLS modules
        if (moduleOpenClass.getName().equals(method.getName())) {
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
    
    private String getMethodFieldName(IOpenMethod method) {

        String methodName = getMethodName(method);

        ISelector<IOpenMethod> nameSel = (ISelector<IOpenMethod>) new ASelector.StringValueSelector(methodName, INamedThing.NAME_CONVERTOR);
        List<IOpenMethod> list = AOpenIterator.select(moduleOpenClass.getMethods().iterator(), nameSel).asList();

        if (list.size() == 1) {
            return methodName + "_Method";
        }

        int index = list.indexOf(method);
        
        return methodName + index + "_Method";
    }

    
    private String getMethodName(IOpenMethod method) {
        return method.getName();
    }
    
    private String getMethodType(IOpenMethod method) {
        return getOpenClassType(method.getType());
    }
    
    private String getOpenClassType(IOpenClass type) {
        return getClassName(type.getInstanceClass());
    }
    
    private String getParamName(String parameterName, int i) {
        return parameterName == null ? "arg" + i : parameterName;
    }
    
    private String getClassName(Class<?> instanceClass) {
        StringBuffer buf = new StringBuffer(30);
        while (instanceClass.isArray()) {
            buf.append("[]");
            instanceClass = instanceClass.getComponentType();
        }

        buf.insert(0, getScalarClassName(instanceClass));
        return buf.toString();
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
    
    private String getScalarClassName(Class<?> instanceClass) {
        return instanceClass.getName();
    }

}
