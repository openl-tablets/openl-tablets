package org.openl.rules.project.instantiation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;

public class SimpleProjectEngineFactory<T> {

    protected final Logger log = LoggerFactory.getLogger(SimpleProjectEngineFactory.class);
    protected final Map<String, Object> externalParameters;
    protected final boolean executionMode;
    protected final ClassLoader classLoader;
    protected final List<Path> projectDependencies;
    protected final Path project;
    protected final Class<?> interfaceClass;
    // lazy initialization.
    protected Class<?> generatedInterfaceClass;
    protected ProjectDescriptor projectDescriptor;

    public static class SimpleProjectEngineFactoryBuilder<T> {
        protected String project;
        protected String workspace;
        protected ClassLoader classLoader;
        protected Class<T> interfaceClass = null;
        protected Map<String, Object> externalParameters = Collections.emptyMap();
        protected boolean executionMode = true;
        protected String[] projectDependencies;

        public SimpleProjectEngineFactoryBuilder<T> setProject(String project) {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty");
            }
            this.project = project;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setInterfaceClass(Class<T> interfaceClass) {
            this.interfaceClass = interfaceClass;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExternalParameters(Map<String, Object> externalParameters) {
            this.externalParameters = Objects.requireNonNullElse(externalParameters, Collections.emptyMap());
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExecutionMode(boolean executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setWorkspace(String workspace) {
            if (workspace == null || workspace.isEmpty()) {
                throw new IllegalArgumentException("workspace cannot be null or empty");
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
                    throw new IllegalArgumentException("Dependency cannot be null or empty");
                }
            }
            this.projectDependencies = projectDependencies;
            return this;
        }

        protected List<Path> getProjectDependencies() {
            var dependencies = new ArrayList<Path>();

            // 1. Handle Workspace
            if (workspace != null) {
                var workspacePath = Path.of(workspace);

                if (!Files.isDirectory(workspacePath)) {
                    throw new IllegalArgumentException("Workspace is not a directory with projects");
                }

                // Files.list throws IOException, so we must catch it.
                // Try-with-resources ensures the stream is closed.
                try (var stream = Files.list(workspacePath)) {
                    stream.filter(Files::isDirectory)
                            .forEach(dependencies::add);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to list workspace directories", e);
                }
            }

            // 2. Handle Explicit Dependencies
            if (projectDependencies != null) {
                for (String dependency : projectDependencies) {
                    var dependencyPath = Path.of(dependency);

                    if (!Files.isDirectory(dependencyPath)) {
                        throw new IllegalArgumentException("Dependency is not a project directory: " + dependency);
                    }

                    dependencies.add(dependencyPath);
                }
            }

            // 3. Return List (Best practice: return empty list rather than null)
            return dependencies;
        }

        public SimpleProjectEngineFactory<T> build() {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty");
            }
            var projectFile = Path.of(project);
            var dependencies = getProjectDependencies();

            return new SimpleProjectEngineFactory<>(projectFile,
                    dependencies,
                    classLoader,
                    interfaceClass,
                    externalParameters,
                    executionMode);
        }
    }

    protected SimpleProjectEngineFactory(Path project,
                                         List<Path> projectDependencies,
                                         ClassLoader classLoader,
                                         Class<T> interfaceClass,
                                         Map<String, Object> externalParameters,
                                         boolean executionMode) {
        this.project = Objects.requireNonNull(project, "project arg cannot be null");
        this.projectDependencies = projectDependencies;
        this.classLoader = classLoader;
        this.interfaceClass = interfaceClass;
        this.externalParameters = externalParameters;
        this.executionMode = executionMode;
    }

    private RulesInstantiationStrategy rulesInstantiationStrategy = null;

    protected RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        if (rulesInstantiationStrategy == null) {
            rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                    dependencyManager,
                    classLoader,
                    isExecutionMode());
        }
        return rulesInstantiationStrategy;
    }

    private Set<ProjectDescriptor> getDependentProjects(ProjectDescriptor project,
                                                        Collection<ProjectDescriptor> projectsInWorkspace) {
        Set<ProjectDescriptor> projectDescriptors = new HashSet<>();
        addDependentProjects(projectDescriptors, project, projectsInWorkspace);
        return projectDescriptors;
    }

    private void addDependentProjects(Set<ProjectDescriptor> projectDescriptors,
                                      ProjectDescriptor project,
                                      Collection<ProjectDescriptor> projectsInWorkspace) {
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                for (ProjectDescriptor projectDescriptor : projectsInWorkspace) {
                    if (dependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        projectDescriptors.add(projectDescriptor);
                        addDependentProjects(projectDescriptors, projectDescriptor, projectsInWorkspace);
                        break;
                    }
                }
            }
        }
    }

    protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
        return new SimpleDependencyManager(buildProjectDescriptors(),
                classLoader,
                isExecutionMode(),
                externalParameters);
    }

    protected Collection<ProjectDescriptor> buildProjectDescriptors() throws ProjectResolvingException {
        Collection<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        ProjectResolver projectResolver = ProjectResolver.getInstance();
        ProjectDescriptor projectDescriptor = getProjectDescriptor();
        if (projectDependencies != null) {
            var projects = new ArrayList<ProjectDescriptor>();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (classLoader != null) {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
                for (var file : projectDependencies) {
                    try {
                        var project = projectResolver.resolve(file);
                        if (project != null) {
                            projects.add(project);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to resolve project in {}", file, ex);
                    }
                }
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            Set<ProjectDescriptor> dependentProjects = getDependentProjects(projectDescriptor, projects);
            projectDescriptors.addAll(dependentProjects);
        }
        projectDescriptors.add(projectDescriptor);
        return projectDescriptors;
    }

    private IDependencyManager dependencyManager = null;

    public synchronized final IDependencyManager getDependencyManager() throws ProjectResolvingException {
        if (dependencyManager == null) {
            dependencyManager = buildDependencyManager();
        }
        return dependencyManager;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    public Class<?> getInterfaceClass() throws RulesInstantiationException, ProjectResolvingException {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (generatedInterfaceClass != null) {
            return generatedInterfaceClass;
        }
        log.info("Interface class is undefined for the factory. Generated interface is used.");
        generatedInterfaceClass = getRulesInstantiationStrategy().getInstanceClass();
        return generatedInterfaceClass;
    }

    @SuppressWarnings("unchecked")
    public T newInstance() throws RulesInstantiationException, ProjectResolvingException {
        return (T) getRulesInstantiationStrategy().instantiate();
    }

    public final synchronized ProjectDescriptor getProjectDescriptor() throws ProjectResolvingException {
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
                        String.format("Failed to resolve the project. Folder '%s' is not a OpenL project.",
                                project.toAbsolutePath()));
            }
            this.projectDescriptor = pd;
        }
        return this.projectDescriptor;
    }

    public final synchronized RulesInstantiationStrategy getRulesInstantiationStrategy() throws RulesInstantiationException,
            ProjectResolvingException {
        if (rulesInstantiationStrategy == null) {
            RulesInstantiationStrategy instantiationStrategy = getStrategy(getProjectDescriptor().getModules(),
                    getDependencyManager());

            Map<String, Object> parameters = ProjectExternalDependenciesHelper
                    .buildExternalParamsWithProjectDependencies(externalParameters, getProjectDescriptor());

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

    public CompiledOpenClass getCompiledOpenClass() throws RulesInstantiationException, ProjectResolvingException {
        return getRulesInstantiationStrategy().compile();
    }

}
