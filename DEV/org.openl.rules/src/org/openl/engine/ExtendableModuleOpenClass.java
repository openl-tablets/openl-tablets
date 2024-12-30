package org.openl.engine;

import org.openl.syntax.code.IParsedCode;

public interface ExtendableModuleOpenClass {
    void applyToDependentParsedCode(IParsedCode parsedCode);
}
