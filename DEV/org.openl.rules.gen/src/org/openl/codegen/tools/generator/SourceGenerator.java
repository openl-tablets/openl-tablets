package org.openl.codegen.tools.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public class SourceGenerator {

    private static final String VELOCITY_PROPERTIES = "velocity.properties";

    private static SourceGenerator instance;

    private VelocityGenerator generator;

    public static SourceGenerator getInstance() throws Exception {

        if (instance == null) {
            instance = new SourceGenerator();
        }

        return instance;
    }

    private SourceGenerator() throws Exception {
        init();
    }

    private void init() throws Exception {

        Properties properties = loadVelocityProperties();

        generator = VelocityGenerator.getInstance(properties);
    }

    private Properties loadVelocityProperties() throws IOException {
        try (FileInputStream is = new FileInputStream(new File(VELOCITY_PROPERTIES))) {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        }
    }

    public void generateSource(String sourceFilePath, String templateName, Map<String, Object> variables)
            throws Exception {

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(sourceFilePath), StandardCharsets.UTF_8)) {
            String codeSnippet = generateSource(templateName, variables);
            writer.write(codeSnippet);
        }
    }

    public String generateSource(String templateName, Map<String, Object> variables) throws Exception {
        return generator.generate(templateName, variables);
    }
}