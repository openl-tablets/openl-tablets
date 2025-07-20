package org.openl.rules.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.ITestUnit;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

public class RulesInFolderTestRunner {
    private final Logger log;
    private final boolean executionMode;
    private final boolean allTestsMustFails;

    public RulesInFolderTestRunner(boolean allTestsMustFails, boolean executionMode) {
        log = LoggerFactory.getLogger(executionMode ? "Compile Rules" : "Test Rules");
        this.executionMode = executionMode;
        this.allTestsMustFails = allTestsMustFails;
    }

    protected CompiledOpenClass validate(CompiledOpenClass compiledOpenClass,
                                         ProjectDescriptor projectDescriptor,
                                         RulesInstantiationStrategy rulesInstantiationStrategy) {
        return compiledOpenClass;
    }

    public boolean run(String path) {
        if (executionMode) {
            log.info(">>> Compiling rules from the directory '{}' in execution mode...", path);
        } else {
            log.info(">>> Compiling rules and running tests from the directory '{}'...", path);
        }
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
            int messagesCount = 0;
            final long startTime = System.nanoTime();
            String sourceFile = file.getName();
            CompiledOpenClass compiledOpenClass;
            if (file.isFile() && (sourceFile.endsWith(".xlsx") || sourceFile.endsWith(".xls"))) {
                try {
                    new FileInputStream(file).close();
                } catch (Exception ex) {
                    error(messagesCount++, startTime, sourceFile, "Failed to read the excel file.", ex);
                    testsFailed = true;
                    continue;
                }

                RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(path + sourceFile);
                engineFactory.setExecutionMode(executionMode);
                compiledOpenClass = engineFactory.getCompiledOpenClass();
            } else if (file.isDirectory()) {
                File[] filesInFolder = file.listFiles();
                boolean multiProject = filesInFolder != null && Arrays.stream(filesInFolder)
                        .allMatch(File::isDirectory);
                try {
                    SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
                    engineFactoryBuilder.setExecutionMode(executionMode);
                    if (multiProject) {
                        engineFactoryBuilder.setWorkspace(file.getPath());
                        for (File f : filesInFolder) {
                            if (Objects.equals(file.getName(), f.getName())) {
                                engineFactoryBuilder.setProject(f.getPath());
                                break;
                            }
                        }
                    } else {
                        engineFactoryBuilder.setProject(file.getPath());
                    }
                    SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                    compiledOpenClass = validate(compiledOpenClass,
                            engineFactory.getProjectDescriptor(),
                            engineFactory.getRulesInstantiationStrategy());
                } catch (ProjectResolvingException | RulesInstantiationException e) {
                    error(messagesCount++, startTime, sourceFile, "Compilation fails.", e);
                    testsFailed = true;
                    continue;
                }
            } else {
                // Skip not a project files
                continue;
            }

            boolean success = true;

            // Check messages
            File msgFile = new File(testsDir, sourceFile + ".msg.txt");
            List<String> expectedMessages = new ArrayList<>();
            if (msgFile.exists() && executionMode) {
                continue;
            }
            if (msgFile.exists()) {
                try (var input = new FileInputStream(msgFile)) {
                    String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    for (String message : content
                            .split("\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]")) {
                        if (!StringUtils.isBlank(message)) {
                            expectedMessages.add(message.trim());
                        }
                    }
                } catch (IOException exc) {
                    error(messagesCount++,
                            startTime,
                            sourceFile,
                            "Failed to read the message file '{}'.",
                            msgFile,
                            exc);
                }

                Collection<OpenLMessage> unexpectedMessages = new LinkedHashSet<>();
                List<String> restMessages = new ArrayList<>(expectedMessages);
                for (OpenLMessage msg : compiledOpenClass.getAllMessages()) {
                    String actual = msg.getSeverity() + ": " + msg.getSummary();
                    if (msg.getSeverity().equals(Severity.ERROR)) {
                        success = false;
                    }
                    Iterator<String> itr = restMessages.iterator();
                    boolean found = false;
                    while (itr.hasNext()) {
                        if (actual.contains(itr.next())) {
                            itr.remove();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        unexpectedMessages.add(msg);
                    }
                }
                if (!unexpectedMessages.isEmpty()) {
                    success = false;
                    error(messagesCount++, startTime, sourceFile, "  UNEXPECTED messages:");
                    for (OpenLMessage msg : unexpectedMessages) {
                        error(messagesCount++,
                                startTime,
                                sourceFile,
                                "   {}: {}    at {}",
                                msg.getSeverity(),
                                msg.getSummary(),
                                msg.getSourceLocation());
                    }
                }
                if (!restMessages.isEmpty()) {
                    success = false;
                    error(messagesCount++, startTime, sourceFile, "  MISSED messages:");
                    for (String msg : restMessages) {
                        error(messagesCount++, startTime, sourceFile, "   {}", msg);
                    }
                }
            }

            // Check compilation
            if (success && compiledOpenClass.hasErrors()) {
                for (OpenLMessage msg : compiledOpenClass.getAllMessages()) {
                    error(messagesCount++,
                            startTime,
                            sourceFile,
                            "   {}: {}    at {}",
                            msg.getSeverity(),
                            msg.getSummary(),
                            msg.getSourceLocation());
                }
                success = false;
            }

            // Run tests
            if (success && !executionMode) {
                IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
                IOpenClass openClass = compiledOpenClass.getOpenClass();
                Object target = openClass.newInstance(env);
                for (IOpenMethod method : openClass.getDeclaredMethods()) {
                    if (method instanceof TestSuiteMethod) {
                        TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                        final int numberOfFailures = res.getNumberOfFailures();
                        if (!allTestsMustFails) {
                            if (numberOfFailures != 0) {
                                error(messagesCount++,
                                        startTime,
                                        sourceFile,
                                        "Failed test: {}  Errors #: {}",
                                        res.getName(),
                                        numberOfFailures);
                                List<ITestUnit> failed = res.getFilteredTestUnits(true, 3);
                                for (ITestUnit testcase : failed) {
                                    error(messagesCount++,
                                            startTime,
                                            sourceFile,
                                            "\n   #{}  \n Actual: {} \n Expected: {}",
                                            testcase.getTest().getId(),
                                            testcase.getActualResult(),
                                            testcase.getExpectedResult());
                                }
                            }
                        } else {
                            if (numberOfFailures != res.getNumberOfTestUnits()) {
                                error(messagesCount++,
                                        startTime,
                                        sourceFile,
                                        "Unexpected test result: {}  Errors #: {}",
                                        res.getName(),
                                        res.getNumberOfTestUnits() - numberOfFailures);
                            }
                        }
                    }
                }
            }

            // Output
            if (messagesCount != 0) {
                testsFailed = true;
            } else {
                ok(startTime, sourceFile);
            }
        }
        return testsFailed;
    }

    private void ok(long startTime, String sourceFile) {
        final long ms = duration(startTime);
        // Green ANSI color
        log.info("\u001B[1;32mOK\u001B[2;36m {}\u001B[0m ({} ms)", sourceFile, ms);
    }

    private void error(int count, long startTime, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            final long ms = duration(startTime);
            // Red ANSI color
            log.error("\u001B[1;31mFAILURE\u001B[2;36m {}\u001B[0m ({} ms)", sourceFile, ms);
        }
        log.error(msg, args);
    }

    private long duration(long startTime) {
        return (System.nanoTime() - startTime) / 1000000;
    }
}
