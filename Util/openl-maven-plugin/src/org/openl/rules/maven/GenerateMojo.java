package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
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
     * Rules module from which Java Beans and the interface are generated. Usually it corresponds to the Excel file name
     * without an extension. If this parameter is not defined, the whole project is used for generating Java classes.
     *
     * @since 5.23.2
     */
    @Parameter
    private String moduleName;

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
            if (StringUtils.isNotEmpty(moduleName)) {
                builder.setModule(moduleName);
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
                if (openLRules.getOpenClass() instanceof XlsModuleOpenClass) {
                    CustomSpreadsheetResultOpenClass spreadsheetResultOpenClass = ((XlsModuleOpenClass) openLRules
                        .getOpenClass()).getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                            .toCustomSpreadsheetResultOpenClass();
                    writeCustomSpreadsheetResultBeans(Collections.singleton(spreadsheetResultOpenClass));
                }
            }

            // Generate interface is optional.
            if (interfaceClass != null) {
                Class<?> interfaceClass = factory.getInterfaceClass();
                IOpenClass openClass = openLRules.getOpenClass();
                writeInterface(interfaceClass, openClass);
                project.addCompileSourceRoot(outputDirectory.getPath());
            }

        } finally {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
    }

    private ClassLoader composeClassLoader() throws Exception {
        info("Composing the classloader for the following sources:");
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

    private void writeJavaBeans(Collection<IOpenClass> types) throws IOException {
        if (CollectionUtils.isNotEmpty(types)) {
            for (IOpenClass openClass : types) {
                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types, csr types).
                //
                if (openClass instanceof DatatypeOpenClass) {
                    Class<?> datatypeClass = openClass.getInstanceClass();
                    String dataType = datatypeClass.getName();
                    info("Java Bean for Datatype: " + dataType);
                    Path filePath = Paths.get(classesDirectory, dataType.replace('.', '/') + ".class");
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, ((DatatypeOpenClass) openClass).getBytecode());
                }
            }
        }
    }

    private void writeCustomSpreadsheetResultBeans(Collection<IOpenClass> types) throws IOException {
        if (CollectionUtils.isNotEmpty(types)) {
            for (IOpenClass openClass : types) {
                // Skip java code generation for other types
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                    Class<?> cls = customSpreadsheetResultOpenClass.getBeanClass();
                    info("Java Bean for Spreadsheet Result: " + cls.getName());
                    Path filePath = Paths.get(classesDirectory, cls.getName().replace('.', '/') + ".class");
                    Files.createDirectories(filePath.getParent());
                    Files.write(filePath, ((CustomSpreadsheetResultOpenClass) openClass).getBeanClassByteCode());
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
        outputDirectory.mkdirs();
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
