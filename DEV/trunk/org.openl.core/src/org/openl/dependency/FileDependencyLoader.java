package org.openl.dependency;

import java.net.MalformedURLException;
import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;

public abstract class FileDependencyLoader implements IDependencyLoader {

    private String openlName;

    public FileDependencyLoader(String openlName) {
        this.openlName = openlName;
    }

    public CompiledDependency load(String dependencyName, IDependencyManager dependencyManager) {
        IOpenSourceCodeModule sourceCode = getSourceCodeModule(dependencyName);

        if (sourceCode != null) {
            OpenL openl = OpenL.getInstance(openlName);
            CompiledOpenClass compiledOpenClass = OpenLManager.compileModuleWithErrors(openl, sourceCode, false, dependencyManager);
            return new CompiledDependency(dependencyName, compiledOpenClass, Thread.currentThread().getContextClassLoader());
        }
        
        return null;
    }

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
