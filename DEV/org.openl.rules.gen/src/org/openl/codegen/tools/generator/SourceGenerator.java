package org.openl.codegen.tools.generator;

import org.openl.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    private Properties loadVelocityProperties() throws IOException, FileNotFoundException {

        Properties properties = new Properties();
        FileInputStream is = new FileInputStream(new File(VELOCITY_PROPERTIES));
        properties.load(is);
        IOUtils.closeQuietly(is);

        return properties;
    }

    public void generateSource(String sourceFilePath, String templateName, Map<String, Object> variables)
            throws Exception {

        File file = new File(sourceFilePath);
        FileOutputStream os = new FileOutputStream(file, false);
        OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
        
        String codeSnippet = generateSource(templateName, variables);
        writer.write(codeSnippet);

        writer.close();
    }

    public String generateSource(String templateName, Map<String, Object> variables) throws Exception {
        return generator.generate(templateName, variables);
    }
}