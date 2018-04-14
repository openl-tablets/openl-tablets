package org.openl.extension.xmlrules.parsing;

import java.util.Collection;
import java.util.List;

import org.openl.extension.xmlrules.ExtensionDescriptor;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnvironmentGridBuilder {

    private EnvironmentGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder,
            XmlRulesModuleSourceCodeModule sourceCodeModule,
            Collection<OpenLMessage> messages) {
        try {
            gridBuilder.addCell("Environment", 2).nextRow();

            List<String> dependencies = sourceCodeModule.getModule().getExtension().getDependencies();
            if (dependencies != null && !dependencies.isEmpty()) {
                // It's expected that dependency to Types module is added in dependent workbooks
                for (String dependency : dependencies) {
                    gridBuilder.addCell("dependency").addCell(dependency).nextRow();
                }
            } else {
                gridBuilder.addCell("dependency");
                String name = sourceCodeModule.getModuleName();
                String moduleName = name.substring(0, name.lastIndexOf(".")) + "." + ExtensionDescriptor.TYPES_WORKBOOK
                    .substring(0, ExtensionDescriptor.TYPES_WORKBOOK.lastIndexOf("."));
                gridBuilder.addCell(moduleName);
                gridBuilder.nextRow();
            }

            gridBuilder.addCell("import");
            gridBuilder.addCell("org.openl.rules.binding.StringOperators.*");
            gridBuilder.nextRow();

            gridBuilder.nextRow();
        } catch (RuntimeException e) {
            Logger log = LoggerFactory.getLogger(EnvironmentGridBuilder.class);
            log.error(e.getMessage(), e);
            messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
            gridBuilder.nextRow();
        }
    }

}
