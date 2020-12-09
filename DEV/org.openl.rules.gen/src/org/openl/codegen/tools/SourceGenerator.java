package org.openl.codegen.tools;

import java.io.Writer;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

final class SourceGenerator {

    private static final VelocityTool TOOL = new VelocityTool();
    private static final VelocityEngine velocityEngine = new VelocityEngine("velocity.properties");

    static void generate(String templateName, Map<String, Object> variables, Writer writer) {
        Template template = velocityEngine.getTemplate(templateName);

        VelocityContext context = new VelocityContext();

        for (Map.Entry<String, Object> var : variables.entrySet()) {
            context.put(var.getKey(), var.getValue());
        }

        context.put("tool", TOOL);
        template.merge(context, writer);
    }
}
