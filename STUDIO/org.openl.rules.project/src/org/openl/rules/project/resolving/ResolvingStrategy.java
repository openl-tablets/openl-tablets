package org.openl.rules.project.resolving;

import java.io.File;
import java.util.List;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Detects whether specified folder is OpenL project or not
 * 
 * @author PUdalau
 */
public interface ResolvingStrategy {
    /**
     * @param folder Project root.
     * @return <code>true</code> if specified folder is OpenL project
     */
    boolean isRulesProject(File folder);

    /**
     * @param folder Project root.
     * @return {@link ProjectDescriptor} that describes project
     */
    ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException;

    List<InitializingModuleListener> getInitializingModuleListeners();

    void addInitializingModuleListener(InitializingModuleListener initializingModuleListener);

    boolean removeInitializingModuleListener(InitializingModuleListener initializingModuleListener);

    void removeAllInitializingModuleListeners();
    
    void setInitializingModuleListeners(List<InitializingModuleListener> initializingModuleListeners);

    List<InitializingProjectListener> getInitializingProjectListeners();

    void addInitializingProjectListener(InitializingProjectListener initializingProjectListener);

    boolean removeInitializingProjectListener(InitializingProjectListener initializingProjectListener);

    void removeAllInitializingProjectListeners();

    void setInitializingProjectListeners(List<InitializingProjectListener> initializingProjectListeners);

}
