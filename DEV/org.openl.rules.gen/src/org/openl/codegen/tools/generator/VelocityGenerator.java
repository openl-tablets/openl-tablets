package org.openl.codegen.tools.generator;

import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Generator class that uses the Velocity engine to generate code snippets. Implemented using singleton pattern.
 * 
 * @author Alexey Gamanovich
 * 
 */
public class VelocityGenerator {

    private VelocityEngine velocityEngine;
    private static VelocityGenerator instance;

    public VelocityGenerator(Properties properties) throws Exception {
        init(properties);
    }

    /**
     * Inits velocity engine.
     * 
     * @throws Exception if an error has occured
     */
    private void init(Properties properties) throws Exception {

        this.velocityEngine = new VelocityEngine();
        this.velocityEngine.init(properties);
    }

    /**
     * Gets the instance of generator.
     * 
     * @return instance object
     * @throws Exception if an error has occured
     */
    public static VelocityGenerator getInstance(Properties properties) throws Exception {

        if (instance == null) {
            instance = new VelocityGenerator(properties);
        }

        return instance;
    }

    /**
     * Generates the string using specified template.
     * 
     * @param templateName template
     * @param variables variables what used in template
     * @return generated string
     * @throws Exception if an error has occured
     */
    public String generate(String templateName, Map<String, Object> variables) throws Exception {

        Template template = velocityEngine.getTemplate(templateName);

        VelocityContext context = new VelocityContext();
        populateContext(context, variables);

        StringWriter writer = new StringWriter();
        template.merge(context, writer);

        return writer.toString();
    }

    private void populateContext(VelocityContext context, Map<String, Object> variables) {

        Set<String> keys = variables.keySet();

        for (String key : keys) {
            Object value = variables.get(key);
            context.put(key, value);
        }
    }
}
