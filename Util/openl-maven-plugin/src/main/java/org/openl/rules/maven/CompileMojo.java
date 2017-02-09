package org.openl.rules.maven;

import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.types.IOpenClass;

/**
 * Compile and validate OpenL project
 * 
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends BaseOpenLMojo {

    @Override
    public String execute(CompiledOpenClass openLRules) throws Exception {
        IOpenClass openClass = openLRules.getOpenClass();
        List<OpenLMessage> messages = openLRules.getMessages();
        List<OpenLMessage> warnings = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        info("Compilation has finished.");
        info("DataTypes: " + openClass.getTypes().size());
        info("Methods  : " + openClass.getMethods().size());
        info("Fields   : " + openClass.getFields().size());
        info("Warnings : " + warnings.size());
        return null;
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }
}
