package org.openl.dependency.loader;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.dependency.CompiledDependency;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * Common implementation to load dependency as file.
 *
 */
public abstract class FileDependencyLoader implements IDependencyLoader {

    private String openlName;
    
    /**
     * Constructor.
     * 
     * @param openlName 
     */
    public FileDependencyLoader(String openlName) {
        this.openlName = openlName;
    }

    public CompiledDependency load(String dependencyName, IDependencyManager dependencyManager) {
        IOpenSourceCodeModule sourceCode = getSourceCodeModule(dependencyName);

        if (sourceCode != null) {
            OpenL openl = OpenL.getInstance(openlName);
            CompiledOpenClass compiledOpenClass = OpenLManager.compileModuleWithErrors(openl, sourceCode, 
                dependencyManager.isExecutionMode(), dependencyManager);
            return new CompiledDependency(dependencyName, compiledOpenClass);
        }
        
        return null;
    }
    
    /**
     * Finds {@link IOpenSourceCodeModule} for given dependency file name.
     * 
     * @param filename dependency file name.
     * @return {@link IOpenSourceCodeModule} for income filename.
     */
    protected IOpenSourceCodeModule getSourceCodeModule(String filename) {

        String fileOrURL = PropertiesLocator.locateFileOrURL(filename);

        IOpenSourceCodeModule source = null;

        if (fileOrURL != null) {
            try {
                if (fileOrURL.indexOf(':') < 2) {
                    source = new FileSourceCodeModule(fileOrURL, null);
                } else {
                    source = new URLSourceCodeModule(new URL(fileOrURL));
                }
            } catch (MalformedURLException e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

        return source;
    }
    
}
