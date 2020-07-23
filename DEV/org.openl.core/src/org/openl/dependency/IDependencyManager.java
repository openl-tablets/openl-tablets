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
     *
     * @throws OpenLCompilationException
     */
    CompiledDependency loadDependency(IDependency dependency) throws OpenLCompilationException;

    /**
     * Remove given dependency from cache.
     *
     * @param dependency to be cleaned from cache.
     */
    void reset(IDependency dependency);

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


    Collection<String> getAllDependencies();
    /**
     * A list of available dependencies.
     */
    Collection<String> getAvailableDependencies();

    void clearOddDataForExecutionMode();

}