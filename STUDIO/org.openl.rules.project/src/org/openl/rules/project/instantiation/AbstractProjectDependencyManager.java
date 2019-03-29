package org.openl.rules.project.instantiation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.openl.OpenClassUtil;
import org.openl.classloader.SimpleBundleClassLoader;
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

public abstract class AbstractProjectDependencyManager extends DependencyManager {

    private LinkedHashSet<DependencyReference> dependencyReferences = new LinkedHashSet<DependencyReference>();

    public LinkedHashSet<DependencyReference> getDependencyReferences() {
        return dependencyReferences;
    }
    
    public static class DependencyReference{
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
            result = prime * result + ((reference == null) ? 0 : reference.hashCode());
            result = prime * result + ((dependency == null) ? 0 : dependency.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DependencyReference other = (DependencyReference) obj;
            if (reference == null) {
                if (other.reference != null)
                    return false;
            } else if (!reference.equals(other.reference))
                return false;
            if (dependency == null) {
                if (other.dependency != null)
                    return false;
            } else if (!dependency.equals(other.dependency))
                return false;
            return true;
        }
    }
    
    private final ClassLoader rootClassLoader;
    
    protected AbstractProjectDependencyManager(ClassLoader rootClassLoader) {
        this.rootClassLoader = rootClassLoader;
    }

    // Disable cache. if cache required it should be used in loaders.
    @Override
    public CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException {
        String dependencyName = dependency.getNode().getIdentifier();
        CompiledDependency compiledDependency = handleLoadDependency(dependency);
        if (compiledDependency == null) {
            if (ProjectExternalDependenciesHelper.isProject(dependencyName)) {
                String projectName = ProjectExternalDependenciesHelper.getProjectName(dependencyName);
                return throwCompilationError(dependency, projectName);
            } else {
                throwCompilationError(dependency, dependencyName);
            }
        }
        return compiledDependency;
    }

    private CompiledDependency throwCompilationError(IDependency dependency, String dependencyName) throws OpenLCompilationException {
        IdentifierNode node = dependency.getNode();
        OpenLCompilationException exception = new OpenLCompilationException(
                String.format("Dependency with name '%s' hasn't been found.", dependencyName),
                null,
                node.getSourceLocation(),
                node.getModule()
        );

        if (node.getParent() instanceof TableSyntaxNode) {
            ((TableSyntaxNode) node.getParent()).addError(SyntaxNodeExceptionUtils.createError(exception, node));
        }

        throw exception;
    }

    private Deque<String> moduleCompilationStack = new ArrayDeque<String>();
    private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();

    public Deque<String> getCompilationStack() {
        return moduleCompilationStack;
    }

    protected abstract Collection<ProjectDescriptor> getProjectDescriptors();

    public ClassLoader getClassLoader(ProjectDescriptor project) {
        getDependencyLoaders();
        if (classLoaders.get(project.getName()) != null) {
            return classLoaders.get(project.getName());
        }
        ClassLoader parentClassLoader = rootClassLoader == null ? this.getClass().getClassLoader() : rootClassLoader;  
        SimpleBundleClassLoader classLoader = new SimpleBundleClassLoader(project.getClassPathUrls(), parentClassLoader);
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
    
    private List<ClassLoader> oldClassLoaders = new ArrayList<ClassLoader>();

    public synchronized void reset(IDependency dependency){
        final String dependencyName = dependency.getNode().getIdentifier();
        for (ProjectDescriptor projectDescriptor : getProjectDescriptors()){
            String projectDependencyName = ProjectExternalDependenciesHelper.buildDependencyNameForProjectName(
                projectDescriptor.getName());
            if (dependencyName.equals(projectDependencyName)){
                for (Module module : projectDescriptor.getModules()){
                    reset(new Dependency(DependencyType.MODULE,
                        new IdentifierNode(dependency.getNode().getType(), null, module.getName(), null)));
                }
                ClassLoader classLoader = classLoaders.get(projectDescriptor.getName());
                if (classLoader != null){
                    oldClassLoaders.add(classLoader);
                }
                classLoaders.remove(projectDescriptor.getName());
                break;
            }
        }
        
        List<DependencyReference> dependenciesToReset = new ArrayList<DependencyReference>();
        List<DependencyReference> dependenciesRefernciesToClear = new ArrayList<DependencyReference>();
        for (DependencyReference dependencyReference : getDependencyReferences()){
            if (dependencyReference.getReference().equals(dependencyName)){
                dependenciesToReset.add(dependencyReference);
            }
            if (dependencyReference.getDependency().equals(dependencyName)){
                dependenciesRefernciesToClear.add(dependencyReference);
            }
        }
        
        for (DependencyReference dependencyReference : dependenciesToReset){
            reset(new Dependency(DependencyType.MODULE,
                new IdentifierNode(dependency.getNode().getType(), null, dependencyReference.getDependency(), null)));
        }
        
        for (IDependencyLoader dependencyLoader : getDependencyLoaders()) {
            SimpleProjectDependencyLoader simpleProjectDependencyLoader = ((SimpleProjectDependencyLoader) dependencyLoader);
            if (simpleProjectDependencyLoader.getDependencyName().equals(dependencyName)){
                for (DependencyReference dependencyReference : dependenciesRefernciesToClear){
                    getDependencyReferences().remove(dependencyReference);
                }
                simpleProjectDependencyLoader.reset();
                break;
            }
        }
    }
    
    public synchronized void resetAll(){
        for (ClassLoader classLoader : oldClassLoaders){
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        oldClassLoaders.clear();
        for (ClassLoader classLoader : classLoaders.values()){
            OpenClassUtil.releaseClassLoader(classLoader);
        }
        classLoaders.clear();
        
        for (IDependencyLoader dependencyLoader : getDependencyLoaders()) {
            ((SimpleProjectDependencyLoader) dependencyLoader).reset();
        }
    }

}
