/**
 * Created Oct 25, 2005
 */
package org.openl.conf.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.openl.CompiledOpenClass;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.UserContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.main.OpenLWrapper;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.meta.IVocabulary;
import org.openl.rules.context.IRulesRuntimeContextConsumer;
import org.openl.rules.context.IRulesRuntimeContextProvider;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.FileTool;
import org.openl.util.StringTool;
import org.openl.util.generation.SimpleBeanJavaGenerator;

/**
 * @author 
 * 
 */
public class JavaWrapperAntTask extends Task {

    private static final String GOAL_MAKE_WRAPPER = "make wrapper";
    private static final String GOAL_UPDATE_PROPERTIES = "update properties";
    private static final String GOAL_MAKE_WEBINF = "make WEB-INF";
    private static final String GOAL_ALL = "all";
    private static final String GOAL_GENERATE_DATATYPES = "generate datatypes";    

    private IOpenClass openClass;

    private String goal = GOAL_ALL;
    private String web_inf_path;
    private String web_inf_exclude = ".*apache.ant.*|.*apache.tomcat.*|.*javacc.*";
    private String web_inf_include = "";

    private String classpathExclude = ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*";
    private String vocabularyClass;

    private String projectHome = ".";
    private boolean ignoreNonJavaTypes = false;

    private String ignoreFields;
    private String ignoreMethods;

    private String userClassPath;
    private String userHome = ".";
    private String deplUserHome;

    private String srcFile; 
    private String deplSrcFile;

    private String srcModuleClass;
    private String openlName;
    private String targetSrcDir;

    private String targetClass;
    private String displayName;

    private String[] methods;
    private String[] fields;

    private String s_package;
    private String s_class;
    
    private String rulesFolder = "rules";

    private String extendsClass = null;

    /*
     * Full or relative path to directory where properties will be saved
     */
    private String classpathPropertiesOutputDir = ".";

    private String[] implementsInterfaces = new String[] { OpenLWrapper.class.getName(),
            IRulesRuntimeContextProvider.class.getName() , IRulesRuntimeContextConsumer.class.getName()};    
    
    @Override
    public void execute() throws BuildException {
        try {
            run();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
    
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
    
    public String getClasspathExclude() {
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

    public String[] getImplementsInterfaces() {
        return implementsInterfaces;
    }    

    public String[] getMethods() {
        return methods;
    }    

    public String getOpenlName() {
        return openlName;
    }

    private String getOutputFileName() {
        String file = targetSrcDir + "/" + targetClass.replace('.', '/') + ".java";
        return file;
    }
    
    public String getRulesFolder() {
        return rulesFolder;
    }    

    public String getS_class() {
        return s_class;
    }

    public String getS_package() {
        return s_package;
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

    public boolean isIgnoreNonJavaTypes() {
        return ignoreNonJavaTypes;
    }

    private IOpenClass makeOpenClass() throws Exception {

        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), userHome);
        if (userClassPath != null) {
            ClassLoader cl = ClassLoaderFactory.createClassLoader(userClassPath, this.getClass().getClassLoader(), ucxt);

            ucxt = new UserContext(cl, userHome);
            Thread.currentThread().setContextClassLoader(cl);
        }

        long start = System.currentTimeMillis();
        OpenClassJavaWrapper jwrapper = null;
        try {
            jwrapper = OpenClassJavaWrapper.createWrapper(openlName, ucxt, srcFile);
        } finally {
            long end = System.currentTimeMillis();
            System.out.println("Loaded " + srcFile + " in " + (end - start) + " ms");
        }
        List<OpenLMessage> errorMessages = 
            OpenLMessagesUtils.filterMessagesBySeverity(((CompiledOpenClass)jwrapper.getCompiledClass()).getMessages(), 
                Severity.ERROR);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            String message = getErrorMessage(errorMessages);
//            throw new OpenLCompilationException(message);            
            System.err.println(message);
        } //else {
            return jwrapper.getOpenClass();
//        }
    }

    private String getErrorMessage(List<OpenLMessage> errorMessages) {
        StringBuffer buf = new StringBuffer();
        buf.append("There are critical errors in wrapper:\n");
        for(int i = 0; i < errorMessages.size(); i++) {
            if (errorMessages.get(i) instanceof OpenLErrorMessage) {
                OpenLErrorMessage openlError = (OpenLErrorMessage)errorMessages.get(i);
                buf.append(openlError.getError().toString());     
                buf.append("\n\n");
            } else {
                // shouldn`t happen
                buf.append(String.format("[%s] %s", i+1, errorMessages.get(i).getSummary()));     
                buf.append("\n");
            }
        }
        return buf.toString();
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
            
            JavaWrapperGenerator javaGenerator = new JavaWrapperGenerator(getTargetClass(), getExtendsClass(), 
                getImplementsInterfaces(), getOpenlName(), getDeplSrcFile(), getSrcFile(), getSrcModuleClass(), getUserHome(),
                getDeplUserHome(), getRulesFolder(), getFields(), getMethods(), isIgnoreNonJavaTypes());
            
            
            String content = javaGenerator.generateJavaClass(openClass);
            String fileName = getOutputFileName();
            writeContentToFile(content, fileName);
        }
        
        if (goal.equals(GOAL_ALL) || goal.contains(GOAL_GENERATE_DATATYPES)) {
            writeDatatypeBeans(openClass.getTypes());
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
 
    private void writeContentToFile(String content, String fileName) throws IOException {
        FileWriter fw = null;
        try {
            new File(fileName).getParentFile().mkdirs();
            fw = new FileWriter(fileName);
            fw.write(content);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    private void writeDatatypeBeans(Map<String, IOpenClass> types) throws Exception {
        if (types != null) {
            for (Entry<String, IOpenClass> datatype : types.entrySet()) {

            	// Skip java code generation for types what is defined 
            	// thru DomainOpenClass (skip java code generation for alias types).
            	// 
            	IOpenClass datatypeOpenClass = datatype.getValue();
                if (!(datatypeOpenClass instanceof DomainOpenClass)) {
	                Class<?> datatypeClass = datatypeOpenClass.getInstanceClass();
                    SimpleBeanJavaGenerator beanJavaGenerator = new SimpleBeanJavaGenerator(datatypeClass,
                            getFieldsDescription(datatypeOpenClass.getDeclaredFields()),
                            getFieldsDescription(datatypeOpenClass.getFields()));
	                String javaClass = beanJavaGenerator.generateJavaClass();
	                String fileName = targetSrcDir + "/" + datatypeClass.getName().replace('.', '/') + ".java";
	                writeContentToFile(javaClass, fileName);
            	}
            }
        }
    }
    
    private Map<String, Class<?>> getFieldsDescription(Map<String, IOpenField> fields) {
        Map<String, Class<?>> fieldsDescriprtion = new LinkedHashMap<String, Class<?>>();
        for (Entry<String, IOpenField> field : fields.entrySet()) {
            if (!field.getValue().isStatic()) {
                fieldsDescriprtion.put(field.getKey(), field.getValue().getType().getInstanceClass());
            }
        }
        return fieldsDescriprtion;
    }

    /**
     * @throws IOException
     * 
     */
    @SuppressWarnings("unchecked")
    private void saveProjectProperties() throws IOException {
        Properties p = new Properties();
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_PROPERTY, filterClassPath());
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_SEPARATOR_PROPERTY, File.pathSeparator);

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

    public void setImplementsInterfaces(String[] implementsInterfaces) {
        List<String> interfaces = new ArrayList<String>(Arrays.asList(this.implementsInterfaces));
        interfaces.addAll(Arrays.asList(implementsInterfaces));
        this.implementsInterfaces = interfaces.toArray(new String[interfaces.size()]);
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

}
