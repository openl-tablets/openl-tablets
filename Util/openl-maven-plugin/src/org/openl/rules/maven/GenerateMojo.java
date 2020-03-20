package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.OpenL;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.maven.decompiler.BytecodeDecompiler;
import org.openl.rules.maven.gen.GenerateInterface;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.openl.util.generation.GenUtils;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.EClassType;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

/**
 * Generates OpenL Tablets interface, domain classes, project descriptor, and unit tests.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public final class GenerateMojo extends BaseOpenLMojo {

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    private List<String> sourceRoots;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, readonly = true)
    private String classesDirectory;

    /**
     * Output directory of the generated Java beans and OpenL Tablets Java interface.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/openl")
    private File outputDirectory;

    /**
     * Comma-separated list of interfaces used for extending the generated interface.
     *
     * @since 5.19.1
     */
    @Parameter
    private String superInterface;

    /**
     * Generated Java interface from an OpenL Tablets project. If it is empty, generation is skipped.
     *
     * @since 5.19.1
     */
    @Parameter
    private String interfaceClass;

    /**
     * Parameter that adds the IRulesRuntimeContext arguments to the generated interface.
     *
     * @since 5.19.1
     */
    @Parameter
    private boolean isProvideRuntimeContext;

    /**
     * Parameter that adds additional methods to the generated interface to support variations.
     *
     * @since 5.19.1
     */
    @Parameter
    private boolean isProvideVariations;

    /**
     * Parameter for generating custom spreadsheet result bean classes.
     *
     * @since 5.23.0
     */
    @Parameter
    private boolean generateSpreadsheetResultBeans;

    /**
     * Additional options for compilation defined externally, such as external dependencies and overridden system
     * properties.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    /**
     * Tasks that will generate classes or data type.
     * <p>
     * <b>Object Properties</b>
     * <table border="1">
     * <tr>
     * <th>Name</th>
     * <th>Type</th>
     * <th>Required</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>srcFile</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Reference to the Excel file for which an interface class must be generated.</td>
     * </tr>
     * <tr>
     * <td>targetClass</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Full name of the interface class to be generated. This parameter is optional if a missed interface is not
     * generated. OpenL Tablets WebStudio recognizes modules in projects by interface classes and uses their names in
     * user interface. If there are multiple wrappers with identical names, only one of them is recognized as a module
     * in OpenL Tablets WebStudio.</td>
     * </tr>
     * <tr>
     * <td>isUsedRuleXmlForGenerate</td>
     * <td>boolean (true/false)</td>
     * <td>false</td>
     * <td>Parameter to enable class and datatype generation from rules.xml. If it is set to yes, srcFile is ignored.
     * targetClass is required for this parameter.</td>
     * </tr>
     * <tr>
     * <td>displayName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>End user-oriented title of the file that appears in OpenL Tablets WebStudio. The default value is the Excel
     * file name without extension.</td>
     * </tr>
     * <tr>
     * <td>targetSrcDir</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Folder where the generated interface class must be saved. Example: "src/main/java". Default value:
     * "${project.build.sourceDirectory}"</td>
     * </tr>
     * <tr>
     * <td>openlName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>OpenL Tablets configuration to be used. For OpenL Tablets, the following value must always be used:
     * org.openl.xls. Default value: "org.openl.xls"</td>
     * </tr>
     * <tr>
     * <td>userHome</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Location of user-defined resources related to the current OpenL Tablets project. Default value: "."</td>
     * </tr>
     * <tr>
     * <td>userClassPath</td>
     * <td>String</td>
     * <td>false</td>
     * <td>Reference to the folder with additional compiled classes imported by the module when the interface is
     * generated. Default value: null.</td>
     * </tr>
     * <tr>
     * <td>ignoreTestMethods</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>Parameter that denotes, if set to true, that test methods will not be added to interface class. It is used
     * only in GenerateInterface. Default value: true.</td>
     * </tr>
     * <tr>
     * <td>generateDataType</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>Parameter that denotes whether dataType must be generated for the current task.</td>
     * </tr>
     * </table>
     * <p>
     *
     * @deprecated It is replaced with the smart generator. Use interfaceClass instead.
     */
    @Parameter
    @Deprecated
    private GenerateInterface[] generateInterfaces;

    /**
     * If this parameter is set to true, rules.xml is generated if it does not exist. If this parameter is set to false,
     * rules.xml is not generated.
     *
     * @see #overwriteProjectDescriptor
     * @deprecated There is no needs to generate rules.xml from Maven.
     */
    @Parameter(defaultValue = "true")
    @Deprecated
    private boolean createProjectDescriptor;

    /**
     * If it is set to true, rules.xml are overwritten on each run. If it is set to false, rules.xml generation is
     * skipped if it exists. Using this parameter makes sense only if createProjectDescriptor == true.
     *
     * @see #createProjectDescriptor
     * @deprecated There is no needs to generate rules.xml from Maven.
     */
    @Parameter(defaultValue = "true")
    @Deprecated
    private boolean overwriteProjectDescriptor;

    /**
     * This parameter identifies a default project name in rules.xml. If omitted, the name of the first module in the
     * project is used. This parameter is used only if createProjectDescriptor == true.
     *
     * @deprecated There is no needs to generate rules.xml from Maven.
     */
    @Parameter
    @Deprecated
    private String projectName;

    /**
     * Default classpath entries in rules.xml. Default value is {"."} It is used only if createProjectDescriptor ==
     * true.
     *
     * @deprecated There is no needs to generate rules.xml from Maven.
     */
    @Parameter
    @Deprecated
    private String[] classpaths = { "." };

    @Override
    @Deprecated
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (generateInterfaces != null) {
            useGenerateInterface();
        } else {
            super.execute();
        }
    }

    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        if (outputDirectory.isDirectory()) {
            info("Cleaning up '", outputDirectory, "' directory...");
            FileUtils.delete(outputDirectory);
        }
        ClassLoader classLoader = null;
        try {
            classLoader = composeClassLoader();

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setProvideRuntimeContext(isProvideRuntimeContext)
                .setProvideVariations(isProvideVariations)
                .setExecutionMode(true)
                .setExternalParameters(externalParameters)
                .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();

            // Generate Java beans from OpenL dataTypes
            writeJavaBeans(openLRules.getTypes());

            if (generateSpreadsheetResultBeans) {
                writeCustomSpreadsheetResultBeans(openLRules.getTypes());
            }

            // Generate interface is optional.
            if (interfaceClass != null) {
                Class<?> interfaceClass = factory.getInterfaceClass();
                IOpenClass openClass = openLRules.getOpenClass();
                writeInterface(interfaceClass, openClass);
            }

            project.addCompileSourceRoot(outputDirectory.getPath());
        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    private ClassLoader composeClassLoader() throws Exception {
        info("Composing the classloader for the folloving sources:");
        for (String dir : sourceRoots) {
            info("  # source roots > ", dir);
        }
        URL[] urls = toURLs(dependencyClasspath(classpath));
        return new URLClassLoader(urls, this.getClass().getClassLoader()) {
            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                String file = name.replace('.', '/').concat(".java");
                for (String dir : sourceRoots) {
                    if (new File(dir, file).isFile()) {
                        debug("  # FOUND > ", dir, "/", file);
                        BeanGenerator builder = new BeanGenerator();
                        builder.setClassLoader(this);
                        builder.setNamingPolicy(new ClassNaming(name));
                        return builder.create().getClass();
                    }
                }
                debug("  > ", file);
                return super.findClass(name);
            }
        };
    }

    private List<String> dependencyClasspath(List<String> classpath) {
        List<String> dependencyClasspath = new ArrayList<>(classpath);
        // No need to use target/classes folder in generate phase. Keep only classpath for dependency jars.
        dependencyClasspath.remove(classesDirectory);
        return dependencyClasspath;
    }

    @Override
    String getHeader() {
        return "OPENL JAVA SOURCES GENERATION";
    }

    @Deprecated
    private void useGenerateInterface() throws MojoExecutionException {
        if (getLog().isInfoEnabled()) {
            getLog().info("Running OpenL GenerateMojo...");
        }
        boolean isUsedRuleXmlForGenerate = false;
        for (GenerateInterface task : generateInterfaces) {
            if (task.isUsedRuleXmlForGenerate()) {
                isUsedRuleXmlForGenerate = true;
                break;
            }
        }
        for (GenerateInterface task : generateInterfaces) {
            if (getLog().isInfoEnabled()) {
                getLog().info(String.format("Generating classes for module '%s'...", task.getDisplayName()));
            }
            initDefaultValues(task, isUsedRuleXmlForGenerate);
            try {
                task.execute();
            } catch (Exception e) {
                throw new MojoExecutionException("Exception during generation: ", e);
            }
        }
        project.addCompileSourceRoot(outputDirectory.getPath());
    }

    private void initDefaultValues(GenerateInterface task, boolean isUsedRuleXmlForGenerate) {
        if (StringUtils.isBlank(task.getResourcesPath())) {
            task.setResourcesPath(getSourceDirectory());
        }
        if (!task.isUsedRuleXmlForGenerate() && isUsedRuleXmlForGenerate) {
            task.setGenerateDataType(false);
        }
        if (task.getOpenlName() == null) {
            task.setOpenlName(OpenL.OPENL_JAVA_RULE_NAME);
        }
        if (task.getTargetSrcDir() == null) {
            task.setTargetSrcDir(outputDirectory.getPath());
        }

        if (task.getDisplayName() == null) {
            task.setDisplayName(FileUtils.getBaseName(task.getSrcFile()));
        }

        if (task.getSrcFile() != null) {
            initResourcePath(task);
        }

        initCreateProjectDescriptorState(task);
        task.setDefaultProjectName(projectName);
        task.setDefaultClasspaths(classpaths);

        task.setLog(getLog());
    }

    private void initCreateProjectDescriptorState(GenerateInterface task) {
        if (createProjectDescriptor) {
            if (new File(task.getResourcesPath(), "rules.xml").exists()) {
                task.setCreateProjectDescriptor(overwriteProjectDescriptor);
                return;
            }
        }
        task.setCreateProjectDescriptor(createProjectDescriptor);
    }

    private void initResourcePath(GenerateInterface task) {
        String srcFile = task.getSrcFile().replace("\\", "/");
        String baseDir = project.getBasedir().getAbsolutePath();

        String directory = getSubDirectory(baseDir, getSourceDirectory()).replace("\\", "/");
        if (srcFile.startsWith(directory)) {
            srcFile = getSubDirectory(directory, srcFile);
            task.setResourcesPath(directory);
            task.setSrcFile(srcFile);
            return;
        }

        List<Resource> resources = project.getResources();
        for (Resource resource : resources) {
            String resourceDirectory = resource.getDirectory();
            resourceDirectory = getSubDirectory(baseDir, resourceDirectory).replace("\\", "/");

            if (srcFile.startsWith(resourceDirectory)) {
                srcFile = getSubDirectory(resourceDirectory, srcFile);
                task.setResourcesPath(resourceDirectory);
                task.setSrcFile(srcFile);
                break;
            }
        }
    }

    private String getSubDirectory(String baseDir, String resourceDirectory) {
        if (resourceDirectory.startsWith(baseDir)) {
            resourceDirectory = resourceDirectory.substring(resourceDirectory.lastIndexOf(baseDir) + baseDir.length());
            resourceDirectory = removeSlashFromBeginning(resourceDirectory);
        }
        return resourceDirectory;
    }

    private String removeSlashFromBeginning(String resourceDirectory) {
        if (resourceDirectory.startsWith("/") || resourceDirectory.startsWith("\\")) {
            resourceDirectory = resourceDirectory.substring(1);
        }
        return resourceDirectory;
    }

    private void writeJavaBeans(Collection<IOpenClass> types) {
        if (CollectionUtils.isNotEmpty(types)) {
            BytecodeDecompiler decompiler = new BytecodeDecompiler(getLog(), outputDirectory);
            for (IOpenClass openClass : types) {
                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types, csr types).
                //
                if (openClass instanceof DatatypeOpenClass) {
                    Class<?> datatypeClass = openClass.getInstanceClass();
                    String dataType = datatypeClass.getName();
                    info("Java Bean for Datatype: " + dataType);
                    decompiler.decompile(dataType, ((DatatypeOpenClass) openClass).getBytecode());
                }
            }
        }
    }

    private void writeCustomSpreadsheetResultBeans(Collection<IOpenClass> types) {
        if (CollectionUtils.isNotEmpty(types)) {
            BytecodeDecompiler decompiler = new BytecodeDecompiler(getLog(), outputDirectory);
            for (IOpenClass openClass : types) {
                // Skip java code generation for other types
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                    Class<?> cls = customSpreadsheetResultOpenClass.getBeanClass();
                    info("Java Bean for Spreadsheet Result: " + cls.getName());
                    decompiler.decompile(cls.getName(), customSpreadsheetResultOpenClass.getBeanClassByteCode());
                }
            }
        }
    }

    private void writeInterface(Class<?> clazz, IOpenClass openClass) throws IOException, JClassAlreadyExistsException {
        info("Interface: " + interfaceClass);
        JCodeModel model = new JCodeModel();
        CodeHelper helper = new CodeHelper();

        // Generate a class body
        JDefinedClass java = model._class(interfaceClass, EClassType.INTERFACE);

        // Add super interfaces
        String[] interfaces = StringUtils.split(superInterface, ',');
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (String s : interfaces) {
                java._extends(helper.get(s));
            }
        }

        // Generate methods
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            Class<?> returnType = method.getReturnType();
            debug("   method: ", returnType, "   ", name, "()");
            JMethod jm = java.method(JMod.NONE, helper.get(returnType), name);
            String[] argNames = GenUtils
                .getParameterNames(method, openClass, isProvideRuntimeContext, isProvideVariations);
            Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                Class<?> argType = argTypes[i];
                String argName = argNames[i];
                debug("      arg:     ", argName, "   ", argType);
                jm.param(helper.get(argType), argName);
            }
        }

        // Write the generated source code
        model.build(outputDirectory, (PrintStream) null);
    }

    private static class ClassNaming implements NamingPolicy {
        private final String className;

        private ClassNaming(String className) {
            this.className = className;
        }

        @Override
        public String getClassName(String s, String s1, Object o, Predicate predicate) {
            return className;
        }
    }

    /**
     * A utility class to convert Java classes in CodeModel class descriptors. It is required for managing generated
     * beans because of they have not a classloader.
     */
    private class CodeHelper {
        JCodeModel model = new JCodeModel();

        AbstractJClass get(Class<?> clazz) throws JClassAlreadyExistsException {
            if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                AbstractJClass arrayType = get(componentType);
                return arrayType.array();
            }
            String clazzName = clazz.getName();
            EClassType eClassType = clazz.isInterface() ? EClassType.INTERFACE : EClassType.CLASS;
            return get(clazzName, eClassType);
        }

        AbstractJClass get(String clazzName) throws JClassAlreadyExistsException {
            return get(clazzName, EClassType.INTERFACE);
        }

        private AbstractJClass get(String clazzName, EClassType eClassType) throws JClassAlreadyExistsException {
            AbstractJClass jArgType = model._getClass(clazzName);
            if (jArgType == null) {
                jArgType = model._class(clazzName, eClassType);
            }
            return jArgType;
        }
    }
}
