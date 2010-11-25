package org.openl.source.loaders;

import org.openl.CompiledOpenClass;
import org.openl.source.IOpenSourceCodeModule;

/**
 * Common interface for all dependency loaders.
 * 
 * @author DLiauchuk
 *
 */
public interface IDependencyLoader {

    CompiledOpenClass getCompiledDependency(String dependency, String rootFileUri);

    boolean isSourceLoader();

    IOpenSourceCodeModule getDependencySource(String dependency, String rootFileUri);

}
