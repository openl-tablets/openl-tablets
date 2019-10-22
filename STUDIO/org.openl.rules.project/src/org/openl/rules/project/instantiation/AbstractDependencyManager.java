package org.openl.rules.project.instantiation;

import java.util.*;

import org.openl.OpenClassUtil;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.DependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.exception.OpenLCompilationException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDependencyManager extends DependencyManager {

    private final Logger log = LoggerFactory.getLogger(AbstractDependencyManager.class);

    private final LinkedHashSet<DependencyReference> dependencyReferences = new LinkedHashSet<>();

    private final ThreadLocal<Deque<String>> compilationStackThreadLocal = ThreadLocal.withInitial(ArrayDeque::new);

    private final Map<String, ClassLoader> classLoaders = new HashMap<>();

    private final ClassLoader rootClassLoader;

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
                if (other.dependency != null) {
                    return false;
                }
            } else if (!dependency.equals(other.dependency)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "DependencyReference [reference=" + reference + ", dependency=" + dependency + "]";
        }

    }

    protected AbstractDependencyManager(ClassLoader rootClassLoader) {
        this.rootClassLoader = rootClassLoader;
    }

    private Deque<String> getCompilationStack() {
        return compilationStackThreadLocal.get();
    }

    private boolean isProjectDependency(IDependency dependency) throws OpenLCompilationException {
        final String dependencyName = dependency.getNode().getIdentifier();
        List<IDependencyLoader> dependencyLoaders = getDependencyLoaders();
        int cnt = 0;
        for (IDependencyLoader dependencyLoader : dependencyLoaders) {
            if (dependencyLoader.isProjectDependency(dependencyName)) {
                cnt++;
            }
        }
        if (cnt > 1) {
            throw new OpenLCompilationException(
                String.format("Multiple dependencies with the same name '%s' are found.", dependencyName));
        }
        return cnt > 0;
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        String dependencyName = dependency.getNode().getIdentifier();

        Deque<String> compilationStack = getCompilationStack();
        try {
            log.debug("Dependency '{}' is contained in compilation stack ('{}').",
                dependencyName,
                compilationStack.contains(dependencyName));
            boolean isCircularDependency = !isProjectDependency(dependency) && compilationStack
                .contains(dependencyName);
            if (!isCircularDependency && !compilationStack.isEmpty()) {
                DependencyReference dr = new DependencyReference(getCompilationStack().getFirst(), dependencyName);
                this.addDependencyReference(dr);
            }

            if (isCircularDependency) {
                throw new OpenLCompilationException(String.format("Circular dependency is detected: %s.",
                    extractCircularDependencyDetails(dependencyName, compilationStack)));
            }

            CompiledDependency compiledDependency = null;
            try {
                compilationStack.push(dependencyName);
                log.debug("Dependency '{}' is added to compilation stack.", dependencyName);
                compiledDependency = handleLoadDependency(dependency);
            } finally {
                compilationStack.poll();
                log.debug("Dependency '{}' is removed from compilation stack.", dependencyName);
            }

            if (compiledDependency == null) {
                if (ProjectExternalDependenciesHelper.isProject(dependencyName)) {
                    String projectName = ProjectExternalDependenciesHelper.getProjectName(dependencyName);
                    return throwCompilationError(dependency, projectName);
                } else {
                    throwCompilationError(dependency, dependencyName);
                }
            }
            return compiledDependency;
        } finally {
            if (compilationStack.isEmpty()) {
                compilationStackThreadLocal.remove(); // Clean thread
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

    protected abstract Collection<ProjectDescriptor> getProjectDescriptors();

    public synchronized ClassLoader getClassLoader(ProjectDescriptor project) {
        getDependencyLoaders();
        if (classLoaders.get(project.getName()) != null) {
            return classLoaders.get(project.getName());
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;
        OpenLBundleClassLoader classLoader = new OpenLBundleClassLoader(project.getClassPathUrls(), parentClassLoader);
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor projectDependencyDescriptor : project.getDependencies()) {
                if (getProjectDescriptors() != null) {
                    for (ProjectDescriptor projectDescriptor : getProjectDescriptors()) {
                        if (projectDependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                            classLoader.addClassLoader(getClassLoader(projectDescriptor));
                            break;
                        }
                    }
                }
            }
        }

        classLoaders.put(project.getName(), classLoader);
        return classLoader;
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
                .buildDependencyNameForProjectName(projectDescriptor.getName());
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

        for (IDependencyLoader dependencyLoader : getDependencyLoaders()) {
            SimpleDependencyLoader simpleProjectDependencyLoader = (SimpleDependencyLoader) dependencyLoader;
            if (simpleProjectDependencyLoader.getDependencyName().equals(dependencyName)) {
                for (DependencyReference dependencyReference : dependenciesReferenciesToClear) {
                    dependencyReferences.remove(dependencyReference);
                }
                simpleProjectDependencyLoader.reset();
                break;
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

        for (IDependencyLoader dependencyLoader : getDependencyLoaders()) {
            ((SimpleDependencyLoader) dependencyLoader).reset();
        }
    }

    protected synchronized void addDependencyReference(DependencyReference dr) {
        dependencyReferences.add(dr);
    }

}
