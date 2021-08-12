package org.openl.rules.webstudio.dependencies;

import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

public class CompilationInterruptedOpenLErrorMessage extends OpenLMessage {

    public CompilationInterruptedOpenLErrorMessage() {
        super("Compilation process is interrupted because some modules is out of date.", Severity.ERROR);
    }

}
