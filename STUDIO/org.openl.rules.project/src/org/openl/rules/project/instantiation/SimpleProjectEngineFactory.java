package org.openl.rules.project.instantiation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class SimpleProjectEngineFactory<T> implements ProjectEngineFactory<T> {

    private final Log log = LogFactory.getLog(SimpleProjectEngineFactory.class);

    private boolean singleModuleMode = false;
    private Map<String, Object> externalParameters = new HashMap<String, Object>();
    private boolean provideRuntimeContext = false;
    private Class<?> interfaceClass = null;
    private File workspace;
    private File project;
    private ProjectDescriptor projectDescriptor;

    public SimpleProjectEngineFactory(String project) {
        this(new File(project));
    }

    public SimpleProjectEngineFactory(String project, String workspace) {
        if (project == null) {
            throw new IllegalArgumentException("project arg can't be null!");
        }
        File workspaceFile = new File(workspace);
        if (workspace != null && !workspaceFile.isDirectory()) {
            throw new IllegalArgumentException("workspace should be a directory with projects!");
        }
        this.workspace = workspaceFile;
        this.project = new File(project);
    }

    public SimpleProjectEngineFactory(String project, Class<T> clazz) {
        this(new File(project));
        setInterfaceClass(clazz);
    }

    public SimpleProjectEngineFactory(String project, String workspace, Class<T> clazz) {
        this(new File(project), new File(workspace));
        setInterfaceClass(clazz);
    }

    public SimpleProjectEngineFactory(File project) {
        if (project == null) {
            throw new IllegalArgumentException("project arg can't be null!");
        }
        this.project = project;
    }

    public SimpleProjectEngineFactory(File project, File workspace) {
        this(project);
        if (workspace != null && !workspace.isDirectory()) {
            throw new IllegalArgumentException("workspace should be a directory with projects!");
        }
        this.workspace = workspace;
    }

    protected RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        switch (modules.size()) {
            case 0:
                throw new IllegalStateException("There are no modules to instantiate.");
            case 1:
                return RulesInstantiationStrategyFactory.getStrategy(modules.iterator().next(), true, dependencyManager);
            default:
                return new SimpleMultiModuleInstantiationStrategy(modules, dependencyManager);
        }
    }

    private List<ProjectDescriptor> getDependentProjects(ProjectDescriptor project,
            Collection<ProjectDescriptor> projectsInWorkspace) {
        List<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();
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
                    if (log.isWarnEnabled()) {
                        log.warn(String.format("Dependency '%s' for project '%s' not found",
                            dependencyDescriptor.getName(),
                            project.getName()));
                    }
                }
            }
        }
    }

    protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
        Collection<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();
        boolean workspaceContainsProject = false;
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        if (workspace != null) {
            for (File file : workspace.listFiles()) {
                if (!file.equals(project)) {
                    workspaceContainsProject = true;
                    break;
                }
            }
            projectResolver.setWorkspace(workspace.getPath());
            projectDescriptors.addAll(getDependentProjects(projectDescriptor, projectResolver.listOpenLProjects()));
        }
        if (!workspaceContainsProject) {
            projectDescriptors.add(this.projectDescriptor);
        }
        return new SimpleProjectDependencyManager(projectDescriptors, isSingleModuleMode());
    }

    protected Collection<Module> getModules() {
        return projectDescriptor.getModules();
    }

    public boolean isSingleModuleMode() {
        return singleModuleMode;
    }

    public void setSingleModuleMode(boolean singleModuleMode) {
        this.singleModuleMode = singleModuleMode;
    }

    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public void setProvideRuntimeContext(boolean provideRuntimeContext) {
        this.provideRuntimeContext = provideRuntimeContext;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public void setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    public void setExternalParameters(Map<String, Object> externalParameters) {
        this.externalParameters = externalParameters;
    }

    private void resolveInterface(RulesInstantiationStrategy instantiationStrategy) throws RulesInstantiationException,
                                                                                   ClassNotFoundException {
        if (getInterfaceClass() != null) {
            instantiationStrategy.setServiceClass(getInterfaceClass());
        } else {
            if (log.isInfoEnabled()) {
                log.info("Class is undefined for factory. Generated interface will be used.");
            }
            this.interfaceClass = instantiationStrategy.getInstanceClass();
        }
    }

    @SuppressWarnings("unchecked")
    public T newInstance() throws RulesInstantiationException, ProjectResolvingException, ClassNotFoundException {
        if (this.projectDescriptor == null) {
            RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
            ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(project);
            if (resolvingStrategy == null) {
                throw new ProjectResolvingException("Defined location is not a OpenL project.");
            }
            ProjectDescriptor pd = resolvingStrategy.resolveProject(project);
            this.projectDescriptor = pd;
        }

        RulesInstantiationStrategy instantiationStrategy = null;
        instantiationStrategy = getStrategy(getModules(), buildDependencyManager());
        if (isProvideRuntimeContext()) {
            instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
        }

        Map<String, Object> parameters = new HashMap<String, Object>(externalParameters);
        if (!isSingleModuleMode()) {
            parameters = ProjectExternalDependenciesHelper.getExternalParamsWithProjectDependencies(externalParameters,
                getModules());
        }
        instantiationStrategy.setExternalParameters(parameters);
        try {
            resolveInterface(instantiationStrategy);
        } catch (Exception ex) {
            throw new RulesInstantiationException(ex);
        }
        return (T) instantiationStrategy.instantiate();
    }
}
