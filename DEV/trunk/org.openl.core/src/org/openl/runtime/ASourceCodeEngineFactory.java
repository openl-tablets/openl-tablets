package org.openl.runtime;

import java.io.File;
import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.conf.IUserContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

public abstract class ASourceCodeEngineFactory extends AOpenLEngineFactory {

    private IOpenSourceCodeModule sourceCode;

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, IUserContext userContext) {
        super(openlName, userContext);
        this.sourceCode = sourceCode;
    }

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, String userHome) {
        super(openlName, userHome);
        this.sourceCode = sourceCode;
    }

    public ASourceCodeEngineFactory(String openlName, IOpenSourceCodeModule sourceCode) {
        super(openlName);
        this.sourceCode = sourceCode;
    }

    public ASourceCodeEngineFactory(String openlName, String sourceFile) {
        this(openlName, new FileSourceCodeModule(sourceFile, null));
    }

    public ASourceCodeEngineFactory(String openlName, File file) {
        this(openlName, new FileSourceCodeModule(file, null));
    }

    public ASourceCodeEngineFactory(String openlName, URL source) {
        this(openlName, new URLSourceCodeModule(source));
    }

    public synchronized IOpenSourceCodeModule getSourceCode() {
        return sourceCode;
    }
    
    protected CompiledOpenClass initializeOpenClass() {
        CompiledOpenClass compiledOpenClass = OpenLManager.compileModuleWithErrors(getOpenL(), getSourceCode());
        return compiledOpenClass;
    }

}
