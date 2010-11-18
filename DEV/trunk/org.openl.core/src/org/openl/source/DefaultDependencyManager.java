package org.openl.source;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PathTool;

/**
 * Draft. Searches for dependencies in root module path.
 * @author DLiauchuk
 *
 */
public class DefaultDependencyManager implements IDependencyManager {
    
    private Map<IOpenSourceCodeModule, Set<IOpenSourceCodeModule>> dependencyMap = 
        new HashMap<IOpenSourceCodeModule, Set<IOpenSourceCodeModule>>();
    
    public DefaultDependencyManager() {    
    }

    /**
     * Searches dependencies in root module path.
     */
    public IOpenSourceCodeModule find(String dependency, String searchPath) {
        IOpenSourceCodeModule dependencySource = null;
        try {
            String newURL = PathTool.mergePath(searchPath, dependency);
            dependencySource = new URLSourceCodeModule(new URL(newURL));
        } catch (Throwable t) {
            // can`t find dependency
            // do smth
        }
        return dependencySource;
    }

    public boolean addDependenciesSources(IOpenSourceCodeModule moduleSource, Set<IOpenSourceCodeModule> dependentSources) {
        boolean result = false;
        if (!dependencyMap.containsKey(moduleSource) && dependentSources != null) {
            dependencyMap.put(moduleSource, new HashSet<IOpenSourceCodeModule>(dependentSources));
            result = true;
        } 
        return result;
    }

    public Set<IOpenSourceCodeModule> getDependenciesSources(IOpenSourceCodeModule moduleSource) {    
        Set<IOpenSourceCodeModule> sources = dependencyMap.get(moduleSource);
        if (sources != null) {
            return new HashSet<IOpenSourceCodeModule>(sources);
        } else {
            return new HashSet<IOpenSourceCodeModule>();
        }
    }
}
