package org.openl.dependency;

import java.util.Collection;
import java.util.Map;

import org.openl.exception.OpenLCompilationException;
import org.openl.syntax.code.IDependency;

/**
 * Interface for dependency managers that handles loading dependent modules for projects.
 *
 */
public interface IDependencyManager {

    /**
     * Load and compile the given dependency.
     *
     * @param dependency to be loaded.
     * @return {@link CompiledDependency}
     */
    CompiledDependency loadDependency(ResolvedDependency dependency) throws OpenLCompilationException;

    Collection<ResolvedDependency> resolveDependency(IDependency dependency,
            boolean withWildcardSupport) throws AmbiguousDependencyException, DependencyNotFoundException;

    /**
     * Remove given dependency from cache.
     *
     * @param dependency to be cleaned from cache.
     */
    void reset(ResolvedDependency dependency);

    /**
     * Remove all dependencies from cache except the given dependencies and dependencies that are required for the given
     * dependencies.
     *
     * @param dependencies to be saved in cache, removes others
     */

    void resetOthers(ResolvedDependency... dependencies);

    /**
     * Remove all dependencies from cache.
     */
    void resetAll();

    /**
     * Return flag, describing is it execution mode or not.<br>
     * In execution mode all meta info that is not used in rules running is being cleaned.
     *
     * @return flag is it execution mode or not.
     */
    boolean isExecutionMode();

    /**
     * Some additional options for compilation defined externally(e.g. external dependencies, overridden system
     * properties)
     */
    Map<String, Object> getExternalParameters();
}