package org.openl.rules.project.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RuntimeContextInstantiationStrategyEnhancer;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.resolving.ProjectResource;
import org.openl.rules.project.resolving.ProjectResourceLoader;
import org.openl.rules.project.validation.base.ValidatedCompiledOpenClass;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationFactoryHelper;
import org.openl.rules.ruleservice.core.interceptors.DynamicInterfaceAnnotationEnhancerHelper;

public abstract class AbstractServiceInterfaceProjectValidator implements ProjectValidator {
    private static final String RULES_DEPLOY_XML = "rules-deploy.xml";

    private boolean provideRuntimeContext = true;
    private boolean provideVariations = false;

    private final IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private RulesDeploy rulesDeploy;

    protected ProjectResource loadProjectResource(ProjectResourceLoader projectResourceLoader,
            ProjectDescriptor projectDescriptor,
            String name) {
        ProjectResource[] projectResources = projectResourceLoader.loadResource(name);
        return Arrays.stream(projectResources)
            .filter(e -> Objects.equals(e.getProjectDescriptor().getName(), projectDescriptor.getName()))
            .findFirst()
            .orElse(null);
    }

    protected RulesDeploy loadRulesDeploy(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        ProjectResourceLoader projectResourceLoader = new ProjectResourceLoader(compiledOpenClass);
        ProjectResource projectResource = loadProjectResource(projectResourceLoader,
            projectDescriptor,
            RULES_DEPLOY_XML);
        if (projectResource != null) {
            try {
                return rulesDeploySerializer.deserialize(new FileInputStream(new File(projectResource.getFile())));
            } catch (FileNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    protected RulesDeploy getRulesDeploy(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        if (rulesDeploy == null) {
            rulesDeploy = loadRulesDeploy(projectDescriptor, compiledOpenClass);
        }
        return rulesDeploy;
    }

    private ClassLoader resolveServiceClassLoader(
            RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException {
        ClassLoader moduleGeneratedClassesClassLoader = ((XlsModuleOpenClass) instantiationStrategy.compile()
            .getOpenClassWithErrors()).getClassGenerationClassLoader();
        OpenLBundleClassLoader openLBundleClassLoader = new OpenLBundleClassLoader(null);
        openLBundleClassLoader.addClassLoader(moduleGeneratedClassesClassLoader);
        openLBundleClassLoader.addClassLoader(instantiationStrategy.getClassLoader());
        return openLBundleClassLoader;
    }

    protected Class<?> resolveInterface(ProjectDescriptor projectDescriptor,
            RulesInstantiationStrategy rulesInstantiationStrategy,
            ValidatedCompiledOpenClass validatedCompiledOpenClass) throws RulesInstantiationException {
        RulesDeploy rulesDeploy = getRulesDeploy(projectDescriptor, validatedCompiledOpenClass);
        if (rulesDeploy != null && rulesDeploy.getServiceName() != null) {
            final String serviceClassName = rulesDeploy.getServiceName().trim();
            if (!StringUtils.isEmpty(serviceClassName)) {
                try {
                    return validatedCompiledOpenClass.getClassLoader().loadClass(serviceClassName);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils
                        .newWarnMessage(String.format("Failed to load a service class '%s'.", serviceClassName)));
                }
            }
        }
        final boolean provideRuntimeContext = rulesDeploy == null && isProvideRuntimeContext() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideRuntimeContext());
        final boolean provideVariations = rulesDeploy == null && isProvideVariations() || rulesDeploy != null && Boolean.TRUE
            .equals(rulesDeploy.isProvideVariations());
        if (provideVariations) {
            rulesInstantiationStrategy = new VariationInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        if (provideRuntimeContext) {
            rulesInstantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(rulesInstantiationStrategy);
        }
        String annotationTemplateClassName = null;
        if (rulesDeploy != null) {
            annotationTemplateClassName = rulesDeploy.getAnnotationTemplateClassName() != null ? rulesDeploy
                .getAnnotationTemplateClassName() : rulesDeploy.getInterceptingTemplateClassName();
            if (annotationTemplateClassName != null) {
                annotationTemplateClassName = annotationTemplateClassName.trim();
            }
        }
        Class<?> serviceClass = rulesInstantiationStrategy.getInstanceClass();
        ClassLoader classLoader = resolveServiceClassLoader(rulesInstantiationStrategy);
        if (!StringUtils.isEmpty(annotationTemplateClassName)) {
            try {
                Class<?> annotationTemplateClass = classLoader.loadClass(annotationTemplateClassName);
                if (annotationTemplateClass.isInterface()) {
                    serviceClass = DynamicInterfaceAnnotationEnhancerHelper
                        .decorate(serviceClass, annotationTemplateClass, classLoader);
                } else {
                    validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                            "Failed to apply annotation template class '%s'. Interface is expected, but class is found.",
                            annotationTemplateClassName)));
                }
            } catch (Exception | NoClassDefFoundError ignored) {
                validatedCompiledOpenClass.addValidationMessage(OpenLMessagesUtils.newWarnMessage(String
                    .format("Failed to load or apply annotation template class '%s'.", annotationTemplateClassName)));
            }
        }
        return RuleServiceInstantiationFactoryHelper.buildInterfaceForService(rulesInstantiationStrategy.compile()
            .getOpenClassWithErrors(), serviceClass, classLoader, provideRuntimeContext, provideVariations);
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public boolean isProvideVariations() {
        return provideVariations;
    }

    public void setProvideVariations(boolean provideVariations) {
        this.provideVariations = provideVariations;
    }
}
