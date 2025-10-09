package org.openl.rules.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.EClassType;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.exceptions.JCodeModelException;
import com.helger.jcodemodel.writer.JCMWriter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyType;
import org.openl.dependency.ResolvedDependency;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.ruleservice.core.RuleServiceOpenLServiceInstantiationHelper;
import org.openl.rules.ruleservice.publish.common.MethodUtils;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.NullOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

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
     * Rules module from which Java Beans and the interface is generated. Usually it corresponds to the Excel file name
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

            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setProvideRuntimeContext(isProvideRuntimeContext)
                    .setProvideVariations(isProvideVariations)
                    .setExecutionMode(true)
                    .setExternalParameters(externalParameters)
                    .build();

            CompiledOpenClass compiledOpenClass;
            // TODO Support project name
            if (StringUtils.isNotEmpty(moduleName) && interfaceClass == null) {
                try {
                    Collection<ResolvedDependency> resolvedDependencies = factory.getDependencyManager()
                            .resolveDependency(
                                    new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, moduleName, null)),
                                    false);
                    CompiledDependency compiledDependency = factory.getDependencyManager()
                            .loadDependency(resolvedDependencies.iterator().next());
                    compiledOpenClass = compiledDependency.getCompiledOpenClass();
                } catch (OpenLCompilationException e) {
                    Collection<OpenLMessage> messages = new LinkedHashSet<>();
                    for (OpenLMessage openLMessage : OpenLMessagesUtils.newErrorMessages(e)) {
                        String message = String
                                .format("Failed to load module '%s': %s", moduleName, openLMessage.getSummary());
                        messages.add(new OpenLMessage(message, Severity.ERROR));
                    }
                    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(classLoader);
                    try {
                        compiledOpenClass = new CompiledOpenClass(NullOpenClass.the, messages);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldClassLoader);
                    }
                }
            } else {
                compiledOpenClass = factory.getCompiledOpenClass();
            }

            // Generate Java beans from OpenL dataTypes
            writeJavaBeans(compiledOpenClass.getTypes());

            if (generateSpreadsheetResultBeans) {
                if (compiledOpenClass.getOpenClass() instanceof XlsModuleOpenClass) {
                    writeCustomSpreadsheetResultBeans((XlsModuleOpenClass) compiledOpenClass.getOpenClass());
                }
            }

            // Generate interface is optional.
            if (interfaceClass != null) {
                Class<?> interfaceClass = factory.getInterfaceClass();
                writeInterface(interfaceClass, factory.newInstance());
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
                String className = name.replace('.', '/');
                String file = className.concat(".java");
                for (String dir : sourceRoots) {
                    if (new File(dir, file).isFile()) {
                        debug("  # FOUND > ", dir, "/", file);
                        byte[] bytes = generateStubClass(className);
                        return defineClass(name, bytes, 0, bytes.length);
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
                if (openClass instanceof DatatypeOpenClass && ((DatatypeOpenClass) openClass).getBytecode() != null) {
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

    private void writeCustomSpreadsheetResultBeans(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
                                                   Set<IOpenClass> writtenSpreadsheetResultOpenClasses) throws IOException {
        if (!writtenSpreadsheetResultOpenClasses.contains(customSpreadsheetResultOpenClass)) {
            Class<?> cls = customSpreadsheetResultOpenClass.getBeanClass();
            info("Java Bean for Spreadsheet Result: " + cls.getName());
            Path filePath = Paths.get(classesDirectory, cls.getName().replace('.', '/') + ".class");
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, customSpreadsheetResultOpenClass.getBeanClassByteCode());
            writtenSpreadsheetResultOpenClasses.add(customSpreadsheetResultOpenClass);
            for (IOpenField openField : customSpreadsheetResultOpenClass.getFields()) {
                if (openField.getType() instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass1 = (CustomSpreadsheetResultOpenClass) openField
                            .getType();
                    writeCustomSpreadsheetResultBeans(customSpreadsheetResultOpenClass1,
                            writtenSpreadsheetResultOpenClasses);
                } else if (openField.getType() instanceof SpreadsheetResultOpenClass) {
                    SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) openField
                            .getType();
                    writeCustomSpreadsheetResultBeans(spreadsheetResultOpenClass.toCustomSpreadsheetResultOpenClass(),
                            writtenSpreadsheetResultOpenClasses);
                }
            }
        }
    }

    private void writeCustomSpreadsheetResultBeans(XlsModuleOpenClass xlsModuleOpenClass) throws IOException {
        if (xlsModuleOpenClass != null) {
            Set<IOpenClass> writtenSpreadsheetResultOpenClasses = new HashSet<>();
            for (IOpenClass openClass : xlsModuleOpenClass.getTypes()) {
                // Skip java code generation for other types
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
                    writeCustomSpreadsheetResultBeans(customSpreadsheetResultOpenClass,
                            writtenSpreadsheetResultOpenClasses);
                }
            }
            if (xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes() != null) {
                writeCustomSpreadsheetResultBeans(
                        xlsModuleOpenClass.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                .toCustomSpreadsheetResultOpenClass(),
                        writtenSpreadsheetResultOpenClasses);
            }
        }
    }

    private void writeInterface(Class<?> clazz, Object service) throws IOException, JCodeModelException {
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
            IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(method, service);

            String[] argNames = MethodUtils
                    .getParameterNames(openMember, method, isProvideRuntimeContext, isProvideVariations);
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
        new JCMWriter(model).build(outputDirectory, x -> {});
    }

    /**
     * A utility class to convert Java classes in CodeModel class descriptors. It is required for managing generated
     * beans because of they have not a classloader.
     */
    private static class CodeHelper {
        JCodeModel model = new JCodeModel();

        AbstractJClass get(Class<?> clazz) throws JCodeModelException {
            if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                AbstractJClass arrayType = get(componentType);
                return arrayType.array();
            }
            String clazzName = clazz.getName();
            EClassType eClassType = clazz.isInterface() ? EClassType.INTERFACE : EClassType.CLASS;
            return get(clazzName, eClassType);
        }

        AbstractJClass get(String clazzName) throws JCodeModelException {
            return get(clazzName, EClassType.INTERFACE);
        }

        private AbstractJClass get(String clazzName, EClassType eClassType) throws JCodeModelException {
            AbstractJClass jArgType = model._getClass(clazzName);
            if (jArgType == null) {
                jArgType = model._class(clazzName, eClassType);
            }
            return jArgType;
        }
    }

    private static byte[] generateStubClass(String className) {
            ClassWriter classWriter = new ClassWriter(0);
            classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);
            classWriter.visitEnd();
            return classWriter.toByteArray();
    }
}
