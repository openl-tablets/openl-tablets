package org.openl.source;

import java.util.List;

import org.openl.CompiledOpenClass;

/**
 * Draft.
 * @author DLiauchuk
 *
 */
public interface IDependencyManager {

    List<CompiledOpenClass> getDependencies();

    void addDependency(CompiledOpenClass dependency);

    void process(IOpenSourceCodeModule dependencySource);

    IOpenSourceCodeModule find(String dependency);

}