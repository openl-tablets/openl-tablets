package org.openl.rules.maven;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.openl.conf.ant.JavaInterfaceAntTask;
import org.openl.rules.ui.ProjectHelper;
import org.openl.types.IOpenMethod;
import org.openl.util.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GenerateInterface extends JavaInterfaceAntTask {
    private Log log;
    private String testSourceDirectory;

    private Boolean generateUnitTests;
    private String unitTestTemplatePath;
    private Boolean overwriteUnitTests;

    public GenerateInterface() {
        // TODO setGoal() should be refactored: now it's usage is inconvenient
        // and unclear.
        // For interface generation only "generate datatypes" goal is needed
        // Can be overridden in maven configuration
        setGoal(GOAL_GENERATE_DATATYPES);
        setIgnoreTestMethods(true);
    }

    public void setGenerateUnitTests(Boolean generateUnitTests) {
        this.generateUnitTests = generateUnitTests;
    }

    public Boolean getGenerateUnitTests() {
        return generateUnitTests;
    }

    public void setUnitTestTemplatePath(String unitTestTemplatePath) {
        this.unitTestTemplatePath = unitTestTemplatePath;
    }

    public String getUnitTestTemplatePath() {
        return unitTestTemplatePath;
    }

    public Boolean getOverwriteUnitTests() {
        return overwriteUnitTests;
    }

    public void setOverwriteUnitTests(Boolean overwriteUnitTests) {
        this.overwriteUnitTests = overwriteUnitTests;
    }

    protected void setTestSourceDirectory(String testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    protected void setLog(Log log) {
        this.log = log;
    }

    @Override
    protected void writeSpecific() {
        super.writeSpecific();
        if (generateUnitTests) {
            generateTests();
        }
    }

    private void generateTests() {
        if (log.isInfoEnabled()) {
            log.info(String.format("Generating unit tests for module '%s'...", getSrcFile()));
        }
        VelocityEngine ve = new VelocityEngine();
        Template template;
        try {
            ve.setProperty("resource.loader", "string");
            ve.setProperty("string.resource.loader.class", StringResourceLoader.class.getName());
            ve.setProperty("string.resource.loader.repository.static", false);
            ve.init();
            if (!ve.resourceExists(unitTestTemplatePath)) {
                StringResourceRepository repo = (StringResourceRepository) ve.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
                repo.putStringResource(unitTestTemplatePath, getTemplateFromResource(unitTestTemplatePath));
            }

            template = ve.getTemplate(unitTestTemplatePath);
        } catch (Exception e) {
            throw new IllegalStateException("Can't find template " + unitTestTemplatePath, e);
        }

        VelocityContext vc = new VelocityContext();

        vc.put("StringUtils", StringUtils.class);
        vc.put("openlInterfacePackage", getPackageName());
        vc.put("openlInterfaceClass", getClassName());
        vc.put("testMethodNames", getTestMethodNames());
        vc.put("projectRoot", StringEscapeUtils.escapeJava(StringUtils.removeEnd(getResourcesPath(), File.separator)));
        vc.put("srcFile", StringEscapeUtils.escapeJava(getSrcFile()));

        StringWriter writer = new StringWriter();

        try {
            template.merge(vc, writer);
            writeContentToFile(writer.toString(), getOutputFileName());
        } catch (IOException e) {
            throw new IllegalStateException("Can't generate JUnit class for file " + getSrcFile(), e);
        }
    }

    private String getClassName() {
        String targetClass = getTargetClass();
        int idx = targetClass.lastIndexOf('.');
        return idx < 0 ? null : targetClass.substring(idx + 1);
    }

    private String getPackageName() {
        String targetClass = getTargetClass();
        int idx = targetClass.lastIndexOf('.');
        return idx < 0 ? null : targetClass.substring(0, idx);
    }

    private List<String> getTestMethodNames() {
        List<String> methodNames = new ArrayList<String>();
        for (IOpenMethod method : ProjectHelper.allTesters(getOpenClass())) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    private String getOutputFileName() {
        return testSourceDirectory + "/" + getTargetClass().replace('.', '/') + "Test.java";
    }

    private void writeContentToFile(String content, String fileName) throws IOException {
        FileWriter fw = null;
        try {
            if (new File(fileName).exists()) {
                if (!overwriteUnitTests) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. Skip it.", fileName));
                        return;
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. Overwrite it.", fileName));
                    }
                }
            }
            File folder = new File(fileName).getParentFile();
            if (!folder.mkdirs() && !folder.exists()) {
                throw new IOException("Can't create folder " + folder.getAbsolutePath());
            }
            fw = new FileWriter(fileName);
            fw.write(content);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    private String getTemplateFromResource(final String templatePath) throws IOException {
        InputStream inputStream;
        if (new File(templatePath).exists()) {
            inputStream = new FileInputStream(templatePath);
        } else {
            inputStream = getClass().getClassLoader().getResourceAsStream(templatePath);
        }
        return IOUtils.toStringAndClose(inputStream);
    }
}
