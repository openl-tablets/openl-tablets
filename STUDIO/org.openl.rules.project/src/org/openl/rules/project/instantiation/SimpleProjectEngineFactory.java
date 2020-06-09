package org.openl.rules.project.instantiation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProjectEngineFactory<T> implements ProjectEngineFactory<T> {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectEngineFactory.class);

    private final boolean singleModuleMode;
    private final Map<String, Object> externalParameters;
    private final boolean provideRuntimeContext;
    private final boolean provideVariations;
    private final boolean executionMode;
    private final String module;
    private final ClassLoader classLoader;
    private final File[] projectDependencies;
    private final File project;
    private final Class<?> interfaceClass;
    // lazy initialization.
    private Class<?> generatedInterfaceClass;
    private ProjectDescriptor projectDescriptor;

    public static class SimpleProjectEngineFactoryBuilder<T> {
        private String project;
        private String workspace;
        private ClassLoader classLoader;
        private String module;
        private boolean provideRuntimeContext = false;
        private boolean provideVariations = false;
        private Class<T> interfaceClass = null;
        private Map<String, Object> externalParameters = Collections.emptyMap();
        private boolean executionMode = true;
        private String[] projectDependencies;

        public SimpleProjectEngineFactoryBuilder<T> setProject(String project) {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty.");
            }
            this.project = project;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setInterfaceClass(Class<T> interfaceClass) {
            this.interfaceClass = interfaceClass;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExternalParameters(Map<String, Object> externalParameters) {
            if (externalParameters != null) {
                this.externalParameters = externalParameters;
            } else {
                this.externalParameters = Collections.emptyMap();
            }
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExecutionMode(boolean executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setModule(String module) {
            if (module == null || module.isEmpty()) {
                throw new IllegalArgumentException("module cannot be null or empty.");
            }
            this.module = module;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setWorkspace(String workspace) {
            if (workspace == null || workspace.isEmpty()) {
                throw new IllegalArgumentException("workspace cannot be null or empty.");
            }
            this.workspace = workspace;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setProjectDependencies(String... projectDependencies) {
            if (projectDependencies == null || projectDependencies.length == 0) {
                return this;
            }
            for (String dependency : projectDependencies) {
                if (dependency == null || dependency.isEmpty()) {
                    throw new IllegalArgumentException("Dependency cannot be null or empty.");
                }
            }
            this.projectDependencies = projectDependencies;
            return this;
        }

        private File[] getProjectDependencies() {
            List<File> dependencies = new ArrayList<>();
            if (workspace != null) {
                File workspaceFile = new File(workspace);
                if (!workspaceFile.isDirectory()) {
                    throw new IllegalArgumentException("Workspace is not a directory with projects.");
                }
                File[] dirs = workspaceFile.listFiles(File::isDirectory);
                if (dirs != null) {
                    dependencies.addAll(Arrays.asList(dirs));
                }
            }
            if (projectDependencies != null) {
                for (String dependency : projectDependencies) {
                    File dependencyFile = new File(dependency);
                    if (!dependencyFile.isDirectory()) {
                        throw new IllegalArgumentException("Dependency is not a project directory");
                    }
                    dependencies.add(dependencyFile);
                }
            }
            return dependencies.isEmpty() ? null : dependencies.toArray(new File[0]);
        }

        public SimpleProjectEngineFactory<T> build() {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty.");
            }
            File projectFile = new File(project);
            File[] dependencies = getProjectDependencies();

            return new SimpleProjectEngineFactory<>(projectFile,
                dependencies,
                classLoader,
                module,
                interfaceClass,
                externalParameters,
                provideRuntimeContext,
                provideVariations,
                executionMode);
        }

    }

    private SimpleProjectEngineFactory(File project,
            File[] projectDependencies,
            ClassLoader classLoader,
            String module,
            Class<T> interfaceClass,
            Map<String, Object> externalParameters,
            boolean provideRuntimeContext,
            boolean provideVariations,
            boolean executionMode) {
        this.project = Objects.requireNonNull(project, "project arg cannot be null");
        this.projectDependencies = projectDependencies;
        this.classLoader = classLoader;
        this.interfaceClass = interfaceClass;
        this.externalParameters = externalParameters;
        this.provideRuntimeContext = provideRuntimeContext;
        this.provideVariations = provideVariations;
        this.executionMode = executionMode;
        this.module = module;
        this.singleModuleMode = module != null;
    }

    private RulesInstantiationStrategy rulesInstantiationStrategy = null;

    protected RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        if (rulesInstantiationStrategy == null) {
            switch (modules.size()) {
                case 0:
                    throw new IllegalStateException("There are no modules to instantiate.");
                case 1:
                    rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                        .getStrategy(modules.iterator().next(), isExecutionMode(), dependencyManager, classLoader);
                    break;
                default:
                    rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                        dependencyManager,
                        classLoader,
                        isExecutionMode());
            }
        }
        return rulesInstantiationStrategy;
    }

    private List<ProjectDescriptor> getDependentProjects(ProjectDescriptor project,
            Collection<ProjectDescriptor> projectsInWorkspace) {
        List<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        addDependentProjects(projectDescriptors, project, projectsInWorkspace);
        return projectDescriptors;
    }

    private void addDependentProjects(List<ProjectDescriptor> projectDescriptors,
            ProjectDescriptor project,
            Collection<ProjectDescriptor> projectsInWorkspace) {
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                boolean found = false;
                for (ProjectDescriptor projectDescriptor : projectsInWorkspace) {
                    if (dependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        projectDescriptors.add(projectDescriptor);
                        addDependentProjects(projectDescriptors, projectDescriptor, projectsInWorkspace);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    log.warn("Dependency '{}' for project '{}' is not found.",
                        dependencyDescriptor.getName(),
                        project.getName());
                }
            }
        }
    }

    protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
        Collection<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        ProjectDescriptor projectDescriptor = getProjectDescriptor();
        if (projectDependencies != null) {
            List<ProjectDescriptor> projects;
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (classLoader != null) {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
                projects = projectResolver.resolve(projectDependencies);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            List<ProjectDescriptor> dependentProjects = getDependentProjects(projectDescriptor, projects);
            projectDescriptors.addAll(dependentProjects);
        }
        projectDescriptors.add(projectDescriptor);
        return new SimpleDependencyManager(projectDescriptors,
            classLoader,
            isSingleModuleMode(),
            isExecutionMode(),
            getExternalParameters());
    }

    private IDependencyManager dependencyManager = null;

    protected synchronized final IDependencyManager getDependencyManager() throws ProjectResolvingException {
        if (dependencyManager == null) {
            dependencyManager = buildDependencyManager();
        }
        return dependencyManager;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    @Override
    public boolean isSingleModuleMode() {
        return singleModuleMode;
    }

    @Override
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public boolean isProvideVariations() {
        return provideVariations;
    }

    @Override
    public Class<?> getInterfaceClass() throws RulesInstantiationException,
                                        ProjectResolvingException,
                                        ClassNotFoundException {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (generatedInterfaceClass != null) {
            return generatedInterfaceClass;
        }
        log.info("Interface class is undefined for factory. Generated interface is used.");
        generatedInterfaceClass = getRulesInstantiationStrategy().getInstanceClass();
        return generatedInterfaceClass;
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T newInstance() throws RulesInstantiationException, ProjectResolvingException {
        return (T) getRulesInstantiationStrategy().instantiate();
    }

    protected final synchronized ProjectDescriptor getProjectDescriptor() throws ProjectResolvingException {
        if (this.projectDescriptor == null) {
            ProjectResolver projectResolver = ProjectResolver.getInstance();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            ProjectDescriptor pd;
            try {
                if (classLoader != null) {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
                pd = projectResolver.resolve(project);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            if (pd == null) {
                throw new ProjectResolvingException(
                    "Failed to resolve project. Defined location is not a OpenL project.");
            }
            this.projectDescriptor = pd;
        }
        return this.projectDescriptor;
    }

    protected final synchronized RulesInstantiationStrategy getRulesInstantiationStrategy() throws RulesInstantiationException,
                                                                                            ProjectResolvingException {
        if (rulesInstantiationStrategy == null) {
            RulesInstantiationStrategy instantiationStrategy = null;
            if (!isSingleModuleMode()) {
                instantiationStrategy = getStrategy(getProjectDescriptor().getModules(), getDependencyManager());
            } else {
                for (Module module : getProjectDescriptor().getModules()) {
                    if (module.getName().equals(this.module)) {
                        Collection<Module> modules = new ArrayList<>();
                        modules.add(module);
                        instantiationStrategy = getStrategy(modules, getDependencyManager());
                        break;
                    }
                }
                if (instantiationStrategy == null) {
                    throw new RulesInstantiationException("Module is not found in project.");
                }
            }

            if (isProvideVariations()) {
                instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
            }

            if (isProvideRuntimeContext()) {
                instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
            }

            Map<String, Object> parameters = new HashMap<>(getExternalParameters());
            if (!isSingleModuleMode()) {
                parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(
                    getExternalParameters(),
                    getProjectDescriptor().getModules());
            }
            instantiationStrategy.setExternalParameters(parameters);
            try {
                if (interfaceClass != null) {
                    instantiationStrategy.setServiceClass(interfaceClass);
                }
            } catch (Exception ex) {
                throw new RulesInstantiationException(ex);
            }
            rulesInstantiationStrategy = instantiationStrategy;
        }
        return rulesInstantiationStrategy;
    }

    @Override
    public CompiledOpenClass getCompiledOpenClass() throws RulesInstantiationException, ProjectResolvingException {
        return getRulesInstantiationStrategy().compile();
    }

}
