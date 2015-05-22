package org.openl.rules.extension.instantiation;

import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.source.IOpenSourceCodeModule;

public class ExtensionEngineFactory<T> extends RulesEngineFactory<T> {
    public ExtensionEngineFactory(String openlName, IOpenSourceCodeModule sourceCode, Class<T> interfaceClass) {
        super(openlName, sourceCode, interfaceClass);
    }
}
