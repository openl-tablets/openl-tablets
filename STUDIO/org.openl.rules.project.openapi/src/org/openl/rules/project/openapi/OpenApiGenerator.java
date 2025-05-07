package org.openl.rules.project.openapi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import jakarta.xml.bind.JAXBException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.openapi.OpenAPIConfiguration;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSOpenLServiceEnhancerHelper;
import org.openl.rules.serialization.ProjectJacksonObjectMapperFactoryBean;
import org.openl.types.IOpenClass;
import org.openl.util.StringUtils;

/**
 * This class generates {@link OpenAPI} model from given OpenL Project.
 *
 * @author Vladyslav Pikus
 */
public class OpenApiGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiGenerator.class);

    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private final Reader reader = new Reader();

    private final ProjectDescriptor projectDescriptor;
    private final RulesInstantiationStrategy instantiationStrategy;
    private final CompiledOpenClass compiledOpenClass;
    private final IOpenClass openClass;
    private final boolean provideRuntimeContext;
    private final boolean provideVariations;

    private final IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private RulesDeploy rulesDeploy;
    private ClassLoader classLoader;

    private OpenApiGenerator(ProjectDescriptor projectDescriptor,
                             RulesInstantiationStrategy instantiationStrategy,
                             boolean provideRuntimeContext,
                             boolean provideVariations) throws RulesInstantiationException {
        this.projectDescriptor = projectDescriptor;
        this.instantiationStrategy = instantiationStrategy;
        this.compiledOpenClass = instantiationStrategy.compile();
        this.openClass = compiledOpenClass.getOpenClass();
        this.provideRuntimeContext = provideRuntimeContext;
        this.provideVariations = provideVariations;
    }

    /**
     * Invokes compilation and generates new instance of {@link OpenAPI} model for given OpenL Project
     *
     * @return new instance of OpenAPI model
     * @throws RulesInstantiationException in case of compilation errors or if project has now public rules tables
     */
    public OpenAPI generate() throws RulesInstantiationException, OpenApiGenerationException {
        final ClassLoader serviceClassLoader = resolveServiceClassLoader(instantiationStrategy);
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(serviceClassLoader);
            RulesInstantiationStrategy enhancedInstantiationStrategy = enhanceRulesInstantiationStrategy(
                    instantiationStrategy,
                    isProvidedRuntimeContext(),
                    isProvideVariations());
            Class<?> serviceClass = resolveInterface(enhancedInstantiationStrategy);
            ObjectMapper objectMapper = createObjectMapper(serviceClassLoader);
            Class<?> enhancedServiceClass = enhanceWithJAXRS(serviceClass,
                    enhancedInstantiationStrategy.instantiate(),
                    serviceClassLoader
            );
            Map<Method, Method> methodMap = buildMethodMapWithJAXRS(serviceClass, enhancedServiceClass);
            if (methodMap.isEmpty()) {
                throw new OpenApiGenerationException(
                        "There are no public methods. Check the provided rules, annotation template class, and included/excluded methods in module settings.");
            }
            return OpenAPIConfiguration.generateOpenAPI(enhancedServiceClass, objectMapper);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    private ProjectResource loadProjectResource(ProjectResourceLoader projectResourceLoader, String name) {
        ProjectResource[] projectResources = projectResourceLoader.loadResource(name);
        return Arrays.stream(projectResources)
                .filter(e -> Objects.equals(e.getProjectDescriptor().getName(), projectDescriptor.getName()))
                .findFirst()
                .orElse(null);
    }

    private RulesDeploy loadRulesDeploy() {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = loadProjectResource(projectResourceLoader, RULES_DEPLOY_XML);
        if (projectResource != null) {
            try {
                return rulesDeploySerializer.deserialize(new FileInputStream(projectResource.getFile()));
            } catch (FileNotFoundException | JAXBException e) {
                LOG.debug("Ignored error: ", e);
                return null;
            }
        }
        return null;
    }

    private RulesDeploy getRulesDeploy() {
        if (rulesDeploy == null) {
            rulesDeploy = loadRulesDeploy();
        }
        return rulesDeploy;
    }

    private ClassLoader resolveServiceClassLoader(
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException {
        if (classLoader == null) {
            ClassLoader moduleGeneratedClassesClassLoader = ((XlsModuleOpenClass) instantiationStrategy.compile()
                    .getOpenClass()).getClassGenerationClassLoader();
            OpenLClassLoader openLClassLoader = new OpenLClassLoader(null);
            openLClassLoader.addClassLoader(moduleGeneratedClassesClassLoader);
            openLClassLoader.addClassLoader(instantiationStrategy.getClassLoader());
            classLoader = openLClassLoader;
        }
        return classLoader;
    }

    protected RulesInstantiationStrategy enhanceRulesInstantiationStrategy(
            RulesInstantiationStrategy rulesInstantiationStrategy,
            boolean provideRuntimeContext,
            boolean provideVariations) {
        if (provideVariations) {
            rulesInstantiationStrategy = new VariationInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        if (provideRuntimeContext) {
            rulesInstantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        return rulesInstantiationStrategy;
    }

    private Class<?> resolveInterface(
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException, OpenApiGenerationException {
        final RulesDeploy rulesDeployValue = getRulesDeploy();
        final Optional<String> serviceClassName = Optional.ofNullable(rulesDeployValue)
                .map(RulesDeploy::getServiceClass)
                .map(StringUtils::trimToNull);

        if (serviceClassName.isPresent()) {
            try {
                Class<?> serviceClass = compiledOpenClass.getClassLoader().loadClass(serviceClassName.get());
                if (serviceClass.isInterface()) {
                    return serviceClass;
                } else {
                    throw new OpenApiGenerationException(String
                            .format("Interface is expected for service class '%s', but class is found.", serviceClassName));
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                throw new OpenApiGenerationException(
                        String.format("An error is occurred during loading a service class '%s'.%s",
                                serviceClassName,
                                StringUtils.isNotBlank(e.getMessage()) ? " " + e.getMessage() : StringUtils.EMPTY));
            }
        }

        final String templateClassName = Optional.ofNullable(rulesDeployValue)
                .map(RulesDeploy::getAnnotationTemplateClassName)
                .map(StringUtils::trimToNull)
                .orElseGet(() -> Optional.ofNullable(rulesDeployValue)
                        .map(RulesDeploy::getInterceptingTemplateClassName)
                        .map(StringUtils::trimToNull)
                        .orElse(null));

        Class<?> serviceClass = instantiationStrategy.getInstanceClass();
        ClassLoader resolveServiceClassLoader = resolveServiceClassLoader(instantiationStrategy);
        if (!StringUtils.isEmpty(templateClassName)) {
            try {
                Class<?> templateClass = resolveServiceClassLoader.loadClass(templateClassName);
                if (templateClass.isInterface() || Modifier.isAbstract(templateClass.getModifiers())) {
                    serviceClass = DynamicInterfaceAnnotationEnhancerHelper.decorate(serviceClass,
                            templateClass,
                            instantiationStrategy.compile().getOpenClass(),
                            resolveServiceClassLoader);
                } else {
                    throw new OpenApiGenerationException(String.format(
                            "Interface or abstract class is expected for annotation template class '%s', but class is found.",
                            templateClassName));
                }
            } catch (RulesInstantiationException e) {
                throw e;
            } catch (Exception | NoClassDefFoundError e) {
                throw new OpenApiGenerationException(
                        String.format("An error is occurred during loading or applying annotation template class '%s'.%s",
                                templateClassName,
                                StringUtils.isNotBlank(e.getMessage()) ? " " + e.getMessage() : StringUtils.EMPTY));
            }
        }
        return RuleServiceInstantiationFactoryHelper.buildInterfaceForService(
                instantiationStrategy.compile().getOpenClass(),
                serviceClass,
                resolveServiceClassLoader,
                instantiationStrategy.instantiate(true),
                isProvidedRuntimeContext(),
                isProvideVariations());
    }

    private boolean isProvidedRuntimeContext() {
        return Optional.ofNullable(getRulesDeploy())
                .map(RulesDeploy::isProvideRuntimeContext)
                .orElse(provideRuntimeContext);
    }

    private boolean isProvideVariations() {
        return Optional.ofNullable(getRulesDeploy()).map(RulesDeploy::isProvideVariations).orElse(provideVariations);
    }

    private ObjectMapper createObjectMapper(ClassLoader serviceClassLoader) {
        ClassLoader classLoader = compiledOpenClass.getClassLoader();

        var objectMapperFactoryBean = new ProjectJacksonObjectMapperFactoryBean();
        objectMapperFactoryBean.setClassLoader(classLoader);
        objectMapperFactoryBean.setRulesDeploy(getRulesDeploy());
        objectMapperFactoryBean.setXlsModuleOpenClass((XlsModuleOpenClass) openClass);
        objectMapperFactoryBean.setClassLoader(serviceClassLoader);
        try {
            return objectMapperFactoryBean.createJacksonObjectMapper();
        } catch (ClassNotFoundException e) { // Never happens
            throw new IllegalStateException("Failed to create an object mapper", e);
        }
    }

    private Class<?> enhanceWithJAXRS(Class<?> originalClass,
                                      Object targetService,
                                      ClassLoader classLoader) throws OpenApiGenerationException {
        try {
            return JAXRSOpenLServiceEnhancerHelper.enhanceInterface(originalClass,
                    targetService,
                    classLoader,
                    isProvidedRuntimeContext(),
                    isProvideVariations()
            );
        } catch (Exception e) {
            throw new OpenApiGenerationException("Failed to build an interface for the project.", e);
        }
    }

    private Map<Method, Method> buildMethodMapWithJAXRS(Class<?> serviceClass,
                                                        Class<?> enhancedServiceClass) throws OpenApiGenerationException {
        try {
            return JAXRSOpenLServiceEnhancerHelper.buildMethodMap(serviceClass, enhancedServiceClass);
        } catch (Exception e) {
            throw new OpenApiGenerationException("Failed to build an interface for the project.", e);
        }
    }

    public static class Builder {

        private final ProjectDescriptor projectDescriptor;
        private final RulesInstantiationStrategy instantiationStrategy;

        private boolean provideRuntimeContext = true;
        private boolean provideVariations;

        private Builder(ProjectDescriptor projectDescriptor, RulesInstantiationStrategy instantiationStrategy) {
            this.projectDescriptor = projectDescriptor;
            this.instantiationStrategy = instantiationStrategy;
        }

        /**
         * If runtime context must be included to OpenAPI schema or not by default. Takes effect only if
         * {@code  isProvideRuntimeContext} is not provided in rules-deploy.xml
         *
         * @param provideRuntimeContext include runtime context to OpenAPI or not
         * @return current builder instance
         */
        public Builder withDefaultProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        /**
         * If variations endpoints must be included to OpenAPI schema or not by default. Takes effect only if
         * {@code isProvideVariations} is not provided in rules-deploy.xml
         *
         * @param provideVariations include variations endpoints to OpenAPI or not
         * @return current builder instance
         */
        public Builder withDefaultProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        /**
         * Creates new instance of {@link OpenApiGenerator}
         *
         * @return new instance of {@link OpenApiGenerator}
         * @throws RulesInstantiationException in case of compilation errors
         */
        public OpenApiGenerator generator() throws RulesInstantiationException {
            return new OpenApiGenerator(projectDescriptor,
                    instantiationStrategy,
                    provideRuntimeContext,
                    provideVariations);
        }
    }

    /**
     * Creates builder class
     *
     * @param projectDescriptor     project descriptor of current OpenL Project
     * @param instantiationStrategy compilation factory of current OpenL Project
     * @return builder instance
     */
    public static Builder builder(ProjectDescriptor projectDescriptor,
                                  RulesInstantiationStrategy instantiationStrategy) {
        return new Builder(projectDescriptor, instantiationStrategy);
    }

}
