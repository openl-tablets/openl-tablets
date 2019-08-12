package org.openl.runtime;

import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.conf.IUserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

//TODO make builder instead of number of constructors.
public abstract class ASourceCodeEngineFactory extends AOpenLEngineFactory {

    private IOpenSourceCodeModule sourceCode;
    private boolean executionMode;
    private IDependencyManager dependencyManager;

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, IUserContext userContext) {
        super(openlName, userContext);
        initSource(sourceCode);
    }

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome) {
        super(openlName, userHome);
        initSource(sourceCode);
    }

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode) {
        super(openlName);
        initSource(sourceCode);
    }

    public ASourceCodeEngineFactory(String openlName, String sourceFile) {
        this(openlName, new URLSourceCodeModule(sourceFile));
    }

    public ASourceCodeEngineFactory(String openlName, String sourceFile, String userHome) {
        this(openlName, new URLSourceCodeModule(sourceFile), userHome);
    }

    public ASourceCodeEngineFactory(String openlName, URL source) {
        this(openlName, new URLSourceCodeModule(source));
    }

    private void initSource(IOpenSourceCodeModule sourceCode) {
        this.sourceCode = sourceCode;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(boolean executionMode) {
        this.executionMode = executionMode;
    }

    public IOpenSourceCodeModule getSourceCode() {
        return sourceCode;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    protected CompiledOpenClass initializeOpenClass() {
        // Change class loader to OpenLBundleClassLoader
        //
        //
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            // if current bundle is dependency of parent bundle it must be
            // visible
            // for parent bundle
            //
            if (!(oldClassLoader instanceof OpenLBundleClassLoader)) {
                ClassLoader newClassLoader = new OpenLBundleClassLoader(oldClassLoader);
                Thread.currentThread().setContextClassLoader(newClassLoader);
            }

            return OpenLManager.compileModuleWithErrors(getOpenL(), getSourceCode(), executionMode, dependencyManager);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
