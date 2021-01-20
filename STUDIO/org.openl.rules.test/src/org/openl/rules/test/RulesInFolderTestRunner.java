package org.openl.rules.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

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
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesInFolderTestRunner {
    private final Logger log = LoggerFactory.getLogger(RulesInFolderTestRunner.class);
    private final boolean executionMode;
    private final boolean allTestsMustFails;

    public RulesInFolderTestRunner(boolean allTestsMustFails, boolean executionMode) {
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
                try {
                    SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
                    engineFactoryBuilder.setExecutionMode(executionMode);
                    engineFactoryBuilder.setProject(file.getPath());
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
                try {
                    String content = IOUtils.toStringAndClose(new FileInputStream(msgFile));
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
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
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
                for (OpenLMessage msg : compiledOpenClass.getMessages()) {
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
                ok(startTime, executionMode, sourceFile);
            }
        }
        return testsFailed;
    }

    private void ok(long startTime, boolean executionMode, String sourceFile) {
        final long ms = (System.nanoTime() - startTime) / 1000000;
        log.info("{} - in [{}] ({} ms)", executionMode ? "EXECUTION MODE COMPILED" : "SUCCESS", sourceFile, ms);
    }

    private void error(int count, long startTime, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            final long ms = (System.nanoTime() - startTime) / 1000000;
            log.error("FAILURE - in [{}] ({} ms)", sourceFile, ms);
        }
        log.error(msg, args);
    }
}
