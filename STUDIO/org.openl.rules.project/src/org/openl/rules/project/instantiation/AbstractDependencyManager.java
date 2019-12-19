package org.openl.rules.project.instantiation;

import java.util.*;
import java.util.stream.Collectors;

import org.openl.CompiledOpenClass;
import org.openl.OpenClassUtil;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyManager implements IDependencyManager {

    private final Logger log = LoggerFactory.getLogger(AbstractDependencyManager.class);

    private volatile Map<String, IDependencyLoader> dependencyLoaders = null;
    private final LinkedHashSet<DependencyReference> dependencyReferences = new LinkedHashSet<>();
    private final ThreadLocal<Deque<String>> compilationStackThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<String, ClassLoader> classLoaders = new HashMap<>();
    private final ClassLoader rootClassLoader;
    protected boolean executionMode;
    private Map<String, Object> externalParameters;

    public static class DependencyReference {
        String reference;
        String dependency;

        public DependencyReference(String dependency, String reference) {
            this.dependency = dependency;
            this.reference = reference;
        }

        public String getDependency() {
            return dependency;
        }

        public String getReference() {
            return reference;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (reference == null ? 0 : reference.hashCode());
            result = prime * result + (dependency == null ? 0 : dependency.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DependencyReference other = (DependencyReference) obj;
            if (reference == null) {
                if (other.reference != null) {
                    return false;
                }
            } else if (!reference.equals(other.reference)) {
                return false;
            }
            if (dependency == null) {
                return other.dependency == null;
            } else {
                return dependency.equals(other.dependency);
            }
        }

        @Override
        public String toString() {
            return String.format("DependencyReference [reference=%s, dependency=%s]", reference, dependency);
        }

    }

    protected AbstractDependencyManager(ClassLoader rootClassLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        this.rootClassLoader = rootClassLoader;
        this.executionMode = executionMode;
        this.externalParameters = new HashMap<>();
        this.externalParameters.put(XlsModuleOpenClass.DISABLED_CLEAN_UP, Boolean.TRUE);
        if (externalParameters != null) {
            this.externalParameters.putAll(externalParameters);
        }
        this.externalParameters = Collections.unmodifiableMap(this.externalParameters);
    }

    public final Map<String, IDependencyLoader> getDependencyLoaders() {
        if (dependencyLoaders == null) {
            synchronized (this) {
                if (dependencyLoaders == null) {
                    dependencyLoaders = initDependencyLoaders();
                }
            }
        }
        return dependencyLoaders;
    }

    protected abstract Map<String, IDependencyLoader> initDependencyLoaders();

    private Deque<String> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    @Override
    public final Collection<String> getAllDependencies() {
        return Collections.unmodifiableSet(getDependencyLoaders().keySet());
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        final String dependencyName = dependency.getNode().getIdentifier();
        IDependencyLoader dependencyLoader = getDependencyLoaders().get(dependencyName);
        if (dependencyLoader == null) {
            throw new OpenLCompilationException(String.format("Dependency '%s' is not found.", dependencyName),
                null,
                dependency.getNode().getSourceLocation());
        }
        Deque<String> compilationStack = getCompilationStack();
        try {
            if (log.isDebugEnabled()) {
                log.debug(
                    compilationStack.contains(dependencyName) ? "Dependency '{}' in compilation stack."
                                                              : "Dependency '{}' is not found in compilation stack.",
                    dependencyName);
            }
            boolean isCircularDependency = !dependencyLoader.isProject() && compilationStack.contains(dependencyName);
            if (!isCircularDependency && !compilationStack.isEmpty()) {
                DependencyReference dr = new DependencyReference(getCompilationStack().getFirst(), dependencyName);
                this.addDependencyReference(dr);
            }

            if (isCircularDependency) {
                throw new OpenLCompilationException(String.format("Circular dependency is detected: %s.",
                    extractCircularDependencyDetails(dependencyName, compilationStack)));
            }

            CompiledDependency compiledDependency;
            try {
                compilationStack.push(dependencyName);
                log.debug("Dependency '{}' is added to compilation stack.", dependencyName);
                compiledDependency = dependencyLoader.getCompiledDependency();
            } finally {
                compilationStack.poll();
                log.debug("Dependency '{}' is removed from compilation stack.", dependencyName);
            }

            if (compiledDependency == null) {
                if (dependencyLoader.isProject()) {
                    return throwCompilationError(dependency, dependencyLoader.getProject().getName());
                } else {
                    return throwCompilationError(dependency, dependencyName);
                }
            }
            return compiledDependency;
        } finally {
            if (compilationStack.isEmpty()) {
                compilationStackThreadLocal.remove(); // Clean thread
                // for (Collection<IDependencyLoader> dependencyLoaders : getDependencyLoaders().values()) {
                for (IDependencyLoader dl : getDependencyLoaders().values()) {
                    if (dl.isCompiled()) {
                        CompiledOpenClass compiledOpenClass = dl.getCompiledDependency().getCompiledOpenClass();
                        IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
                        if (openClass instanceof ComponentOpenClass) {
                            ((ComponentOpenClass) openClass).clearOddDataForExecutionMode();
                        }
                    }
                }
                // }
            }
        }
    }

    private String extractCircularDependencyDetails(String dependencyName, Deque<String> compilationStack) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = compilationStack.iterator();
        sb.append("'").append(dependencyName).append("'");
        while (itr.hasNext()) {
            String s = itr.next();
            sb.insert(0, "' -> ");
            sb.insert(0, s);
            sb.insert(0, "'");
            if (Objects.equals(dependencyName, s)) {
                break;
            }
        }
        return sb.toString();
    }

    private CompiledDependency throwCompilationError(IDependency dependency,
            String dependencyName) throws OpenLCompilationException {
        IdentifierNode node = dependency.getNode();
        OpenLCompilationException exception = new OpenLCompilationException(String
            .format("Dependency '%s' is not found.", dependencyName), null, node.getSourceLocation(), node.getModule());

        if (node.getParent() instanceof TableSyntaxNode) {
            ((TableSyntaxNode) node.getParent()).addError(SyntaxNodeExceptionUtils.createError(exception, node));
        }

        throw exception;
    }

    public synchronized ClassLoader getClassLoader(ProjectDescriptor project) {
        getDependencyLoaders(); // Init dependency loaders
        if (classLoaders.get(project.getName()) != null) {
            return classLoaders.get(project.getName());
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        OpenLBundleClassLoader classLoader = new OpenLBundleClassLoader(project.getClassPathUrls(), parentClassLoader);
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
                    if (projectDependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        classLoader.addClassLoader(getClassLoader(projectDescriptor));
                        break;
                    }
                }
            }
        }

        classLoaders.put(project.getName(), classLoader);
        return classLoader;
    }

    public Collection<ProjectDescriptor> getProjectDescriptors() {
        return getDependencyLoaders().values()
            .stream()
            .filter(IDependencyLoader::isProject)
            .map(IDependencyLoader::getProject)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ClassLoader> oldClassLoaders = new ArrayList<>();

    private void reset(IDependency dependency, Set<String> doNotDoTheSameResetTwice) {
        final String dependencyName = dependency.getNode().getIdentifier();
        if (doNotDoTheSameResetTwice.contains(dependencyName)) {
            return;
        }
        doNotDoTheSameResetTwice.add(dependencyName);
        for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
            String projectDependencyName = ProjectExternalDependenciesHelper
                .buildDependencyNameForProject(projectDescriptor.getName());
            if (dependencyName.equals(projectDependencyName)) {
                ClassLoader classLoader = classLoaders.get(projectDescriptor.getName());
                if (classLoader != null) {
                    oldClassLoaders.add(classLoader);
                }
                classLoaders.remove(projectDescriptor.getName());
                for (Module module : projectDescriptor.getModules()) {
                    reset(
                        new Dependency(DependencyType.MODULE,
                            new IdentifierNode(dependency.getNode().getType(), null, module.getName(), null)),
                        doNotDoTheSameResetTwice);
                }
                break;
            }
        }

        List<DependencyReference> dependenciesToReset = new ArrayList<>();
        List<DependencyReference> dependenciesReferenciesToClear = new ArrayList<>();
        for (DependencyReference dependencyReference : dependencyReferences) {
            if (dependencyReference.getReference().equals(dependencyName)) {
                dependenciesToReset.add(dependencyReference);
            }
            if (dependencyReference.getDependency().equals(dependencyName)) {
                dependenciesReferenciesToClear.add(dependencyReference);
            }
        }

        for (DependencyReference dependencyReference : dependenciesToReset) {
            reset(new Dependency(DependencyType.MODULE,
                new IdentifierNode(dependency.getNode().getType(), null, dependencyReference.getDependency(), null)),
                doNotDoTheSameResetTwice);
        }

        IDependencyLoader dependencyLoader = getDependencyLoaders().get(dependencyName);
        if (dependencyLoader != null) {
            dependencyLoader.reset();
            for (DependencyReference dependencyReference : dependenciesReferenciesToClear) {
                dependencyReferences.remove(dependencyReference);
            }
        }
    }

    @Override
    public synchronized void reset(IDependency dependency) {
        reset(dependency, new HashSet<>());
    }

    @Override
    public synchronized void resetAll() {
        for (ClassLoader classLoader : oldClassLoaders) {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        oldClassLoaders.clear();
        for (ClassLoader classLoader : classLoaders.values()) {
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        classLoaders.clear();

        for (IDependencyLoader dependencyLoader : getDependencyLoaders().values()) {
            dependencyLoader.reset();
        }
    }

    protected synchronized void addDependencyReference(DependencyReference dr) {
        dependencyReferences.add(dr);
    }

    /**
     * In execution mode all meta info that is not used in rules running is being cleaned.
     */
    @Override
    public boolean isExecutionMode() {
        return executionMode;
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

}
