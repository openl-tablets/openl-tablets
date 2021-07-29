package org.openl.rules.project.openapi;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.test.RulesInFolderTestRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OpenAPIGenerationTestRunner {

    private final Logger log = LoggerFactory.getLogger(RulesInFolderTestRunner.class);

    public static final String DIR = "test-resources/functionality/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testAll() {
        assertFalse("Test is failed.", run(DIR));
    }

    public boolean run(String path) {
        log.info(">>> Compiling rules from the directory '{}' in execution mode...", path);
        boolean testsFailed = false;
        final File testsDir = new File(path);

        if (!testsDir.exists()) {
            log.warn("Test folder is not found.");
            return false;
        }
        File[] files = testsDir.listFiles();
        // files = new File[] {new File(testsDir, "EPBDS-10072_ALL_multiple")};
        if (files == null) {
            log.warn("Test folder is not found.");
            return false;
        }

        for (File file : files) {
            AtomicInteger messagesCount = new AtomicInteger(0);
            final long startTime = System.nanoTime();
            String sourceFile = file.getName();
            CompiledOpenClass compiledOpenClass;
            ProjectDescriptor projectDescriptor;
            RulesInstantiationStrategy instantiationStrategy;
            if (file.isDirectory()) {
                try {
                    SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
                    engineFactoryBuilder.setExecutionMode(true);
                    engineFactoryBuilder.setProject(file.getPath());
                    SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                    projectDescriptor = engineFactory.getProjectDescriptor();
                    instantiationStrategy = engineFactory.getRulesInstantiationStrategy();
                } catch (ProjectResolvingException | RulesInstantiationException e) {
                    error(messagesCount.incrementAndGet(), startTime, sourceFile, "Compilation fails.", e);
                    testsFailed = true;
                    continue;
                }
            } else {
                // Skip not a project files
                continue;
            }

            // Check compilation
            if (compiledOpenClass.hasErrors()) {
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
                    error(messagesCount.incrementAndGet(),
                        startTime,
                        sourceFile,
                        "   {}: {}    at {}",
                        msg.getSeverity(),
                        msg.getSummary(),
                        msg.getSourceLocation());
                }
                testsFailed = true;
                continue;
            }

            JsonNode actualNode;
            try {
                String actualOpenAPI = OpenApiGenerator.builder(projectDescriptor, instantiationStrategy)
                    .generator()
                    .generate();
                actualNode = OBJECT_MAPPER.readTree(actualOpenAPI);
            } catch (RulesInstantiationException | JsonProcessingException e) {
                error(messagesCount.incrementAndGet(), startTime, sourceFile, "Open API Generation fails.", e);
                testsFailed = true;
                continue;
            }

            // Check OpenAPI
            String expectOpenAPIFileName = sourceFile + ".openapi.json";
            File expectedOpenAPIFile = new File(testsDir, expectOpenAPIFileName);
            if (!expectedOpenAPIFile.exists()) {
                error(messagesCount.incrementAndGet(),
                    startTime,
                    sourceFile,
                    "Failed to find expected Open API file: " + expectOpenAPIFileName);
                testsFailed = true;
                continue;
            }
            if (expectedOpenAPIFile.exists()) {
                JsonNode expectedNode;
                try {
                    expectedNode = OBJECT_MAPPER.readTree(expectedOpenAPIFile);
                } catch (IOException exc) {
                    error(messagesCount.incrementAndGet(),
                        startTime,
                        sourceFile,
                        "Failed to read Open API file '{}'.",
                        expectedOpenAPIFile,
                        exc);
                    testsFailed = true;
                    continue;
                }

                compareJsonObjects(messagesCount, startTime, sourceFile, expectedNode, actualNode, "");
            }

            // Output
            if (messagesCount.get() != 0) {
                testsFailed = true;
            } else {
                ok(startTime, sourceFile);
            }
        }
        return testsFailed;
    }

    private void ok(long startTime, String sourceFile) {
        final long ms = (System.nanoTime() - startTime) / 1000000;
        log.info("SUCCESS - in [{}] ({} ms)", sourceFile, ms);
    }

    private void error(int count, long startTime, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            final long ms = (System.nanoTime() - startTime) / 1000000;
            log.error("FAILURE - in [{}] ({} ms)", sourceFile, ms);
        }
        log.error(msg, args);
    }

    private void compareJsonObjects(AtomicInteger messagesCount,
            long startTime,
            String sourceFile,
            JsonNode expectedJson,
            JsonNode actualJson,
            String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(messagesCount, startTime, sourceFile, expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            // try to compare by a pattern
            String regExp = expectedJson.asText()
                .replaceAll("\\[", "\\\\[")
                .replaceAll("]", "\\\\]")
                .replaceAll("#+", "[#\\\\d]+")
                .replaceAll("@+", "[@\\\\w]+")
                .replaceAll("\\*+", "[^\uFFFF]*");
            String actualText = actualJson.isTextual() ? actualJson.asText() : actualJson.toString();
            if (!Pattern.compile(regExp).matcher(actualText).matches()) {
                failDiff(messagesCount, startTime, sourceFile, expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
                compareJsonObjects(messagesCount,
                    startTime,
                    sourceFile,
                    expectedJson.get(i),
                    actualJson.get(i),
                    path + "[" + i + "]");
            }
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            expectedJson.fieldNames().forEachRemaining(names::add);
            actualJson.fieldNames().forEachRemaining(names::add);

            for (String name : names) {
                compareJsonObjects(messagesCount,
                    startTime,
                    sourceFile,
                    expectedJson.get(name),
                    actualJson.get(name),
                    path + " > " + name);
            }
        } else {
            failDiff(messagesCount, startTime, sourceFile, expectedJson, actualJson, path);
        }
    }

    private void failDiff(AtomicInteger messagesCount,
            long startTime,
            String sourceFile,
            JsonNode expectedJson,
            JsonNode actualJson,
            String path) {
        error(messagesCount.incrementAndGet(), startTime, sourceFile, "Path: \\" + path);
        log.error("   Expected: {}", expectedJson);
        log.error("   Actual: {}", actualJson);
    }

}
