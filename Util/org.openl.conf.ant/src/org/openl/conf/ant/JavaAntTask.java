package org.openl.conf.ant;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.meta.IVocabulary;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.FileTool;
import org.openl.util.StringTool;
import org.openl.util.generation.SimpleBeanJavaGenerator;

public abstract class JavaAntTask extends Task {

    protected static final String GOAL_MAKE_WRAPPER = "make wrapper";
    protected static final String GOAL_UPDATE_PROPERTIES = "update properties";
    protected static final String GOAL_MAKE_WEBINF = "make WEB-INF";
    protected static final String GOAL_ALL = "all";
    protected static final String GOAL_GENERATE_DATATYPES = "generate datatypes";

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

    /**
     * The root path where resources (such as rules.xml and rules) are located
     * For example in maven: "src/main/openl. Default is "".
     */
    private String resourcesPath = "";
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

    private String dependencyManagerClass;

    /*
     * Full or relative path to directory where properties will be saved
     */
    private String classpathPropertiesOutputDir = ".";

    @Override
    public void execute() throws BuildException {
        try {
            run();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    protected void run() throws Exception {
        if (getIgnoreFields() != null) {
            setFields(StringTool.tokenize(getIgnoreFields(), ", "));
        }

        if (getIgnoreMethods() != null) {
            setMethods(StringTool.tokenize(getIgnoreMethods(), ", "));
        }

        if (getGoal().equals(GOAL_ALL) || getGoal().contains(GOAL_UPDATE_PROPERTIES)) {
            saveProjectProperties();
        }

        setOpenClass(makeOpenClass());

        OpenLToJavaGenerator javaGenerator = getJavaGenerator();

        writeJavaWrapper(javaGenerator);

        if (getGoal().equals(GOAL_ALL) || getGoal().contains(GOAL_GENERATE_DATATYPES)) {
            writeDatatypeBeans(getOpenClass().getTypes());
        }

        writeSpecific();

        if (getGoal().contains(GOAL_MAKE_WEBINF)) {
            if (getWeb_inf_path() == null) {
                throw new RuntimeException("web_inf_path is not set");
            }
        }

        if (getGoal().equals(GOAL_ALL) || getGoal().contains(GOAL_MAKE_WRAPPER)) {
            if (getWeb_inf_path() == null) {
                return;
            }

            makeWebInfPath();
        }
    }

    protected abstract void writeSpecific();

    protected abstract OpenLToJavaGenerator getJavaGenerator();

    private String filterClassPath() throws IOException {
        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        StringBuilder buf = new StringBuilder(300);

        for (int i = 0; i < tokens.length; i++) {
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

    public String getResourcesPath() {
        return resourcesPath;
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

    public String getDependencyManager() {
        return dependencyManagerClass;
    }

    public void setDependencyManager(String dependencyManagerClass) {
        this.dependencyManagerClass = dependencyManagerClass;
    }

    protected void setOpenClass(IOpenClass openClass) {
        this.openClass = openClass;
    }

    protected IOpenClass getOpenClass() {
        return openClass;
    }

    protected IOpenClass makeOpenClass() throws Exception {

        ClassLoader applicationClassLoader = getApplicationClassLoader();

        SimpleBundleClassLoader bundleClassLoader = new SimpleBundleClassLoader(applicationClassLoader);
        UserContext ucxt = getUserContext(bundleClassLoader);
        Thread.currentThread().setContextClassLoader(bundleClassLoader);

        long start = System.currentTimeMillis();
        OpenClassJavaWrapper jwrapper = null;
        try {
            IDependencyManager dependencyManager = instantiateDependencyManager();
            jwrapper = OpenClassJavaWrapper.createWrapper(openlName, ucxt, resourcesPath + srcFile, false, dependencyManager);
        } finally {
            long end = System.currentTimeMillis();
            log("Loaded " + resourcesPath + srcFile + " in " + (end - start) + " ms");
        }
        List<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(
                jwrapper.getCompiledClass().getMessages(), Severity.ERROR);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            String message = getErrorMessage(errorMessages);
            // throw new OpenLCompilationException(message);
            log(message, Project.MSG_ERR);
        } // else {
        return jwrapper.getOpenClass();
        // }
    }

    private UserContext getUserContext(ClassLoader cl) throws Exception {
        return new UserContext(cl, userHome);
    }

    private ClassLoader getApplicationClassLoader() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (userClassPath != null) {
            cl = ClassLoaderFactory.createClassLoader(userClassPath, this.getClass().getClassLoader(), userHome);
        }
        return cl;
    }

    private IDependencyManager instantiateDependencyManager() {
        IDependencyManager dependecyManager = null;
        if (StringUtils.isNotBlank(dependencyManagerClass)) {
            try {
                Class<?> depManagerClass = Class.forName(dependencyManagerClass);
                Constructor<?> constructor = depManagerClass.getConstructor();
                dependecyManager = (IDependencyManager) constructor.newInstance();
            } catch (Exception e) {
                log(e, Project.MSG_DEBUG);
            }
        }
        return dependecyManager;
    }

    private String getErrorMessage(List<OpenLMessage> errorMessages) {
        StringBuilder buf = new StringBuilder();
        buf.append("There are critical errors in wrapper:\n");
        for (int i = 0; i < errorMessages.size(); i++) {
            if (errorMessages.get(i) instanceof OpenLErrorMessage) {
                OpenLErrorMessage openlError = (OpenLErrorMessage) errorMessages.get(i);
                buf.append(openlError.getError().toString());
                buf.append("\n\n");
            } else {
                // shouldn`t happen
                buf.append(String.format("[%s] %s", i + 1, errorMessages.get(i).getSummary()));
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    protected void makeWebInfPath() {
        String targetFolder = web_inf_path;

        String classes_target = targetFolder + "/classes";
        String lib_target = targetFolder + "/lib";

        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        log("Making WEB-INF...");

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches(web_inf_exclude) && !tokens[i].matches(web_inf_include)) {
                continue;
            }
            log(tokens[i]);

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

    protected void writeJavaWrapper(OpenLToJavaGenerator javaGenerator) throws IOException {
        if (javaGenerator != null) {
            String content = javaGenerator.generateJava();
            String fileName = getOutputFileName();
            writeContentToFile(content, fileName);
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

    protected void writeDatatypeBeans(Map<String, IOpenClass> types) throws Exception {
        if (types != null) {
            for (Entry<String, IOpenClass> datatype : types.entrySet()) {

                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types).
                //
                IOpenClass datatypeOpenClass = datatype.getValue();
                if (!(datatypeOpenClass instanceof DomainOpenClass)) {
                    Class<?> datatypeClass = datatypeOpenClass.getInstanceClass();
                    SimpleBeanJavaGenerator beanJavaGenerator = new SimpleBeanJavaGenerator(datatypeClass);
                    String javaClass = beanJavaGenerator.generateJavaClass();
                    String fileName = targetSrcDir + "/" + datatypeClass.getName().replace('.', '/') + ".java";
                    writeContentToFile(javaClass, fileName);
                }
            }
        }
    }

    /**
     * @throws IOException
     * 
     */
    protected void saveProjectProperties() throws IOException {
        Properties p = new Properties();
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_PROPERTY, filterClassPath());
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_SEPARATOR_PROPERTY, File.pathSeparator);

        if (displayName != null) {
            p.put(targetClass + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, displayName);
        }

        if (vocabularyClass != null) {
            try {
                Class<?> c = Class.forName(vocabularyClass);
                c.newInstance();
                if (IVocabulary.class.isAssignableFrom(c)){
                    throw new ClassCastException(vocabularyClass + " doesn't implements IVocabulary.");
                }
            } catch (Throwable t) {
                log("Error occured while trying instantiate vocabulary class:" + vocabularyClass, t, Project.MSG_ERR);
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

    public void setResourcesPath(String resourcesPath) {
        this.resourcesPath = resourcesPath.isEmpty() || resourcesPath.endsWith(File.separator) ? resourcesPath
                                                                                              : resourcesPath + File.separator;
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
