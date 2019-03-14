package org.openl.rules.maven.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.maven.decompiler.BytecodeDecompiler;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

@Deprecated
public class GenerateInterface {
    public static final String GOAL_MAKE_WRAPPER = "make wrapper";
    public static final String GOAL_UPDATE_PROPERTIES = "update properties";
    public static final String GOAL_MAKE_WEBINF = "make WEB-INF";
    public static final String GOAL_ALL = "all";
    public static final String GOAL_GENERATE_DATATYPES = "generate datatypes";
    public static final int MSG_ERR = 0;
    public static final int MSG_INFO = 2;
    public static final int MSG_DEBUG = 4;
    private static final String DEFAULT_CLASSPATH = "./bin";
    private Log log;

    private boolean ignoreTestMethods = true;
    private String defaultProjectName;
    private String[] defaultClasspaths = { GenerateInterface.DEFAULT_CLASSPATH };
    private boolean createProjectDescriptor = true;

    public GenerateInterface() {
        // TODO setGoal() should be refactored: now it's usage is inconvenient
        // and unclear.
        // For interface generation only "generate datatypes" goal is needed
        // Can be overridden in maven configuration
        setGoal(GOAL_GENERATE_DATATYPES);
        setIgnoreTestMethods(true);
    }

    private static CompiledOpenClass createWrapper(String openlName,
            IUserContext userContext,
            String filename,
            IDependencyManager dependencyManager) {
        IOpenSourceCodeModule source = new URLSourceCodeModule(filename);
        OpenL openl = OpenL.getInstance(openlName, userContext);
        CompiledOpenClass openClass = OpenLManager.compileModuleWithErrors(openl, source, false, dependencyManager);

        return openClass;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    protected void writeSpecific() throws Exception {
        if (createProjectDescriptor && getSrcFile() != null) {
            writeRulesXML();
        }
    }

    private void writeContentToFile(String content, String fileName, boolean override) throws IOException {
        FileWriter fw = null;
        try {
            if (new File(fileName).exists()) {
                if (override) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. It has been overwritten.", fileName));
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. I has been skiped.", fileName));
                        return;
                    }
                }
            }
            File folder = new File(fileName).getParentFile();
            if (!folder.mkdirs() && !folder.exists()) {
                throw new IOException("Can't create folder '" + folder.getAbsolutePath() + "'.");
            }
            fw = new FileWriter(fileName);
            fw.write(content);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    protected ProjectDescriptor createNewProject() {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setName(defaultProjectName != null ? defaultProjectName : getDisplayName());

        List<PathEntry> classpath = new ArrayList<PathEntry>();
        for (String path : defaultClasspaths) {
            classpath.add(new PathEntry(path));
        }
        project.setClasspath(classpath);

        return project;
    }

    protected Module createNewModule() {
        Module module = new Module();

        module.setName(getDisplayName());
        module.setRulesRootPath(new PathEntry(getSrcFile()));
        return module;
    }

    // TODO extract the code that writes rules.xml, to another class
    protected void writeRulesXML() {
        File rulesDescriptor = new File(
            getResourcesPath() + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager manager = new ProjectDescriptorManager();

        ProjectDescriptor projectToWrite;
        List<Module> modulesToWrite = new ArrayList<Module>();
        long timeSinceModification = System.currentTimeMillis() - rulesDescriptor.lastModified();

        // FIXME: This is tricky to rely on the time since modification.
        // Consider that if the time since last modification is small enough it
        // will be the modification
        // made for previously created module by this ant task and we need to add one more module to the project
        // @author DLiauchuk
        if (rulesDescriptor.exists() && timeSinceModification < 2000) {
            // There is a previously created project descriptor, with modules in it.
            // The time was small enough to consider that it was modified/created by the generator.
            // Add current module to existed project.
            ProjectDescriptor existedDescriptor;
            try {
                existedDescriptor = manager.readOriginalDescriptor(rulesDescriptor);
                Module newModule = createNewModule();
                boolean exist = false;
                for (Module existedModule : existedDescriptor.getModules()) {
                    if (existedModule.getName().equals(newModule.getName())) {
                        modulesToWrite.add(newModule);
                        exist = true;
                    } else {
                        modulesToWrite.add(copyOf(existedModule));
                    }
                }
                if (!exist) {
                    modulesToWrite.add(newModule);
                }
                projectToWrite = existedDescriptor;
            } catch (Exception e) {
                log("Can't read previously created project descriptor file '" + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME + "'.",
                    e,
                    MSG_ERR);
                throw new IllegalStateException(e);
            }
        } else {
            // Create new project and add new module
            projectToWrite = createNewProject();
            modulesToWrite.add(createNewModule());
        }
        projectToWrite.setModules(modulesToWrite);

        FileOutputStream fous = null;
        try {
            fous = new FileOutputStream(rulesDescriptor);
            manager.writeDescriptor(projectToWrite, fous);
        } catch (Exception e) {
            log("Can't write project descriptor file '" + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME + "'.",
                e,
                MSG_ERR);
        } finally {
            IOUtils.closeQuietly(fous);
        }
    }

    /**
     * Copy the module without {@link Module#getProject()}, as it prevents to Circular dependency.
     * 
     * @param module income module
     * @return copy of income module without project field
     */
    private Module copyOf(Module module) {
        Module copy = new Module();
        copy.setName(module.getName());
        copy.setProperties(module.getProperties());
        copy.setRulesRootPath(module.getRulesRootPath());
        return copy;
    }

    public boolean isIgnoreTestMethods() {
        return ignoreTestMethods;
    }

    public void setIgnoreTestMethods(boolean ignoreTestMethods) {
        this.ignoreTestMethods = ignoreTestMethods;
    }

    public String getDefaultProjectName() {
        return defaultProjectName;
    }

    public void setDefaultProjectName(String defaultProjectName) {
        this.defaultProjectName = defaultProjectName;
    }

    public String[] getDefaultClasspaths() {
        return defaultClasspaths;
    }

    public void setDefaultClasspaths(String[] defaultClasspaths) {
        this.defaultClasspaths = defaultClasspaths;
    }

    public boolean isCreateProjectDescriptor() {
        return createProjectDescriptor;
    }

    public void setCreateProjectDescriptor(boolean createProjectDescriptor) {
        this.createProjectDescriptor = createProjectDescriptor;
    }

    private IOpenClass openClass;

    private String goal = GenerateInterface.GOAL_ALL;
    private String web_inf_path;
    private String web_inf_exclude = ".*apache.ant.*|.*apache.tomcat.*|.*javacc.*";
    private String web_inf_include = "";

    private String classpathExclude = ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*";

    private String projectHome = ".";
    private boolean ignoreNonJavaTypes = false;

    private String ignoreFields;
    private String ignoreMethods;

    private String userClassPath;
    private String userHome = ".";
    private String deplUserHome;

    /**
     * The root path where resources (such as rules.xml and rules) are located For example in maven: "src/main/openl.
     * Default is "".
     */
    private String resourcesPath = "";
    private String srcFile;
    private boolean usedRuleXmlForGenerate;
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
    private boolean generateDataType = true;

    /*
     * Full or relative path to directory where properties will be saved
     */
    private String classpathPropertiesOutputDir = ".";

    public void execute() throws Exception {
        if (getIgnoreFields() != null) {
            setFields(StringTool.tokenize(getIgnoreFields(), ", "));
        }

        if (getIgnoreMethods() != null) {
            setMethods(StringTool.tokenize(getIgnoreMethods(), ", "));
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal()
            .contains(GenerateInterface.GOAL_UPDATE_PROPERTIES)) {
            saveProjectProperties();
        }

        writeSpecific();

        setOpenClass(makeOpenClass());

        // Generate interface is optional.
        if (targetClass != null) {
            JavaInterfaceGenerator javaGenerator = new JavaInterfaceGenerator.Builder(getOpenClass(), getTargetClass())
                .methodsToGenerate(getMethods())
                .fieldsToGenerate(getFields())
                .ignoreNonJavaTypes(isIgnoreNonJavaTypes())
                .ignoreTestMethods(isIgnoreTestMethods())
                .build();

            String content = javaGenerator.generateJava();
            String fileName = targetSrcDir + "/" + targetClass.replace('.', '/') + ".java";
            writeContentToFile(content, fileName, true);
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal()
            .contains(GenerateInterface.GOAL_GENERATE_DATATYPES) && isGenerateDataType()) {
            writeDatatypeBeans(getOpenClass().getTypes());
        }

        if (getGoal().contains(GenerateInterface.GOAL_MAKE_WEBINF)) {
            if (getWeb_inf_path() == null) {
                throw new RuntimeException("web_inf_path is not set");
            }
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal().contains(GenerateInterface.GOAL_MAKE_WRAPPER)) {
            if (getWeb_inf_path() == null) {
                return;
            }

            makeWebInfPath();
        }
    }

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
     * Get full or relative path to directory where classpath properties will be save
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

    public IOpenClass getOpenClass() {
        return openClass;
    }

    public IOpenClass makeOpenClass() throws Exception {
        CompiledOpenClass compiledOpenClass = null;

        if (usedRuleXmlForGenerate) {
            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
            SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder
                .setExecutionMode(false)
                .setProvideRuntimeContext(false)
                .setWorkspace(resourcesPath)
                .setProject(resourcesPath)
                .build();

            compiledOpenClass = simpleProjectEngineFactory.getCompiledOpenClass();
        } else {
            ClassLoader applicationClassLoader = getApplicationClassLoader();

            SimpleBundleClassLoader bundleClassLoader = new SimpleBundleClassLoader(applicationClassLoader);
            UserContext ucxt = new UserContext(bundleClassLoader, userHome);
            Thread.currentThread().setContextClassLoader(bundleClassLoader);

            long start = System.currentTimeMillis();
            try {
                IDependencyManager dependencyManager = instantiateDependencyManager();
                compiledOpenClass = createWrapper(openlName, ucxt, resourcesPath + srcFile, dependencyManager);
            } finally {
                long end = System.currentTimeMillis();
                log("Loaded '" + resourcesPath + srcFile + "' in " + (end - start) + " ms");
            }
        }

        Collection<OpenLMessage> errorMessages = OpenLMessagesUtils
            .filterMessagesBySeverity(compiledOpenClass.getMessages(), Severity.ERROR);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            String message = getErrorMessage(errorMessages);
            // throw new OpenLCompilationException(message);
            log(message, GenerateInterface.MSG_ERR);
        }

        return compiledOpenClass.getOpenClass();
        // }
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
                log(e, GenerateInterface.MSG_DEBUG);
            }
        }
        return dependecyManager;
    }

    private String getErrorMessage(Collection<OpenLMessage> errorMessages) {
        StringBuilder buf = new StringBuilder();
        buf.append("There are critical errors in wrapper:\n");
        int i = 0;
        for (OpenLMessage message : errorMessages) {
            if (message instanceof OpenLErrorMessage) {
                OpenLErrorMessage openlError = (OpenLErrorMessage) message;
                buf.append(openlError.getError().toString());
                buf.append("\n\n");
            } else {
                // shouldn`t happen
                buf.append(String.format("[%s] %s", i + 1, message.getSummary()));
                buf.append("\n");
            }
            i++;
        }
        return buf.toString();
    }

    protected void makeWebInfPath() throws IOException {
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
                FileUtils.copy(f, new File(classes_target));
            } else {
                FileUtils.copy(f, new File(lib_target));
            }
        }
    }

    protected void writeDatatypeBeans(Collection<IOpenClass> types) throws Exception {
        if (types != null) {
            BytecodeDecompiler decompiler = new BytecodeDecompiler(log, targetSrcDir);
            for (IOpenClass datatypeOpenClass : types) {

                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types).
                //
                if (datatypeOpenClass instanceof DatatypeOpenClass) {
                    Class<?> datatypeClass = datatypeOpenClass.getInstanceClass();
                    decompiler.decompile(datatypeClass.getName(), ((DatatypeOpenClass) datatypeOpenClass).getBytecode());
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

        new OpenLProjectPropertiesLoader().saveProperties(classpathPropertiesOutputDir, p, false);
    }

    protected void log(String msg) {
        log(msg, GenerateInterface.MSG_INFO);
    }

    protected void log(Exception e, int msgDebug) {
        log(e.getMessage(), msgDebug);
    }

    protected void log(String msg, int msgLevel) {
        if (msgLevel <= GenerateInterface.MSG_INFO) {
            System.err.println(msg);
        }
    }

    protected void log(String s, Throwable t, int msgErr) {
        log(s, msgErr);
        log(t.getMessage(), msgErr);
    }

    public void setClasspathExclude(String classpathExclude) {
        this.classpathExclude = classpathExclude;
    }

    /*
     * Set full or relative path to directory where classpath properties will be save
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
        this.resourcesPath = resourcesPath.isEmpty() || resourcesPath
            .endsWith(File.separator) ? resourcesPath : resourcesPath + File.separator;
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

    public void setWeb_inf_exclude(String web_inf_exclude) {
        this.web_inf_exclude = web_inf_exclude;
    }

    public void setWeb_inf_include(String web_inf_include) {
        this.web_inf_include = web_inf_include;
    }

    public void setWeb_inf_path(String web_inf_path) {
        this.web_inf_path = web_inf_path;
    }

    public boolean isUsedRuleXmlForGenerate() {
        return usedRuleXmlForGenerate;
    }

    public void setUsedRuleXmlForGenerate(boolean usedRuleXmlForGenerate) {
        this.usedRuleXmlForGenerate = usedRuleXmlForGenerate;
    }

    public boolean isGenerateDataType() {
        return generateDataType;
    }

    public void setGenerateDataType(boolean generateDataType) {
        this.generateDataType = generateDataType;
    }
}
