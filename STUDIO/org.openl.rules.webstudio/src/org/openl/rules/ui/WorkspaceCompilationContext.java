package org.openl.rules.ui;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.openl.dependency.CompiledDependency;
import org.openl.meta.IMetaInfo;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.instantiation.AbstractDependencyManager;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceDependencyManagerFactory;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;

/**
 * Session-shared compilation infrastructure for parallel multi-project editing. Owns ONE
 * {@link WebStudioWorkspaceRelatedDependencyManager} for the whole session, expanded to the union of all opened
 * projects' dependency graphs.
 *
 * <p>Because compiled dependencies are cached by name in that single manager, opening project B reuses B's
 * compiled modules already produced while compiling project A (which depends on B) — no redundant
 * recompilation. Editing one project invalidates it and its dependents through the manager's own dependency
 * graph.
 *
 * <p>Also keeps the index of compiled module syntax nodes per project, populated by a single compilation
 * listener. Each {@link ProjectModel} reads the entries for the projects in its own dependency graph.
 *
 * <p>The manager is created lazily on first compile and shut down only when the whole session is reset or
 * destroyed — never when an individual project closes, since other projects share it.
 */
class WorkspaceCompilationContext {

    private final WebStudioWorkspaceDependencyManagerFactory factory;

    private WebStudioWorkspaceRelatedDependencyManager dependencyManager;

    private final Map<String, Set<XlsModuleSyntaxNode>> syntaxNodesPerProject = new ConcurrentHashMap<>();
    private final Collection<XlsModuleSyntaxNode> syntaxNodes = ConcurrentHashMap.newKeySet();

    WorkspaceCompilationContext(WebStudio studio) {
        this.factory = new WebStudioWorkspaceDependencyManagerFactory(studio);
    }

    /**
     * Ensure the shared dependency manager exists and covers the given project's dependency graph, expanding it
     * with any projects it does not cover yet. Returns the shared manager.
     */
    synchronized WebStudioWorkspaceRelatedDependencyManager ensureCovers(ProjectDescriptor projectDescriptor) {
        if (dependencyManager == null) {
            dependencyManager = factory.buildDependencyManager(projectDescriptor);
            dependencyManager.registerOnCompilationCompleteListener(this::onCompiled);
            dependencyManager.registerOnResetCompleteListener(this::onReset);
        } else {
            Set<String> covered = dependencyManager.getDependencyLoaders()
                    .stream()
                    .filter(IDependencyLoader::isProjectLoader)
                    .map(loader -> loader.getProject().getName())
                    .collect(Collectors.toSet());
            Set<ProjectDescriptor> missing = factory.resolveWorkspace(projectDescriptor)
                    .stream()
                    .filter(project -> !covered.contains(project.getName()))
                    .collect(Collectors.toSet());
            if (!missing.isEmpty()) {
                dependencyManager.expand(missing);
            }
        }
        return dependencyManager;
    }

    /**
     * Invalidate a project's compiled modules in the shared manager so a later reopen recompiles them from
     * disk instead of serving cached classes. Used when a project is closed or switched to another
     * branch/revision; the shared manager is kept for the other opened projects, and the reset cascades to
     * projects that depend on this one.
     */
    synchronized void invalidate(ProjectDescriptor descriptor) {
        if (dependencyManager == null) {
            return;
        }
        for (Module module : descriptor.getModules()) {
            dependencyManager.reset(AbstractDependencyManager.buildResolvedDependency(module));
        }
    }

    Set<XlsModuleSyntaxNode> syntaxNodesByProject(String projectName) {
        return syntaxNodesPerProject.computeIfAbsent(projectName, e -> ConcurrentHashMap.newKeySet());
    }

    Collection<XlsModuleSyntaxNode> syntaxNodes() {
        return syntaxNodes;
    }

    Map<String, Set<XlsModuleSyntaxNode>> syntaxNodesPerProject() {
        return syntaxNodesPerProject;
    }

    /**
     * Shut down the shared manager and clear the syntax-node index. Called only on whole-session reset/destroy.
     */
    synchronized void shutdown() {
        if (dependencyManager != null) {
            dependencyManager.shutdown();
            dependencyManager = null;
        }
        syntaxNodesPerProject.clear();
        syntaxNodes.clear();
    }

    private void onCompiled(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        XlsModuleSyntaxNode node = xlsModuleNode(compiledDependency);
        if (node != null) {
            syntaxNodesByProject(dependencyLoader.getProject().getName()).add(node);
            if (!(node.getModule() instanceof VirtualSourceCodeModule)) {
                syntaxNodes.add(node);
            }
        }
    }

    private void onReset(IDependencyLoader dependencyLoader, CompiledDependency compiledDependency) {
        XlsModuleSyntaxNode node = xlsModuleNode(compiledDependency);
        if (node != null) {
            syntaxNodesByProject(dependencyLoader.getProject().getName()).remove(node);
            if (!(node.getModule() instanceof VirtualSourceCodeModule)) {
                syntaxNodes.remove(node);
            }
        }
    }

    private static XlsModuleSyntaxNode xlsModuleNode(CompiledDependency compiledDependency) {
        IMetaInfo metaInfo = compiledDependency.getCompiledOpenClass().getOpenClassWithErrors().getMetaInfo();
        return metaInfo instanceof XlsMetaInfo xlsMetaInfo ? xlsMetaInfo.getXlsModuleNode() : null;
    }
}
