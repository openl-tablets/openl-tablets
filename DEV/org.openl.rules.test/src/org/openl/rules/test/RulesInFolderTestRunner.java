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
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.ProjectDescriptor;
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
        // Skip not a project files
        File[] files = testsDir.listFiles(RulesInFolderTestRunner::isRulesSource);
        if (files == null) {
            log.warn("Test folder is not found.");
            return false;
        }

        for (File file : files) {
            if (new SourceCheck(file.getName()).isFailed(path, testsDir, file)) {
                testsFailed = true;
            }
        }
        return testsFailed;
    }

    /**
     * Checks whether the file holds rules to compile: an Excel file or a project folder.
     */
    private static boolean isRulesSource(File file) {
        String name = file.getName();
        return file.isFile() && (name.endsWith(".xlsx") || name.endsWith(".xls")) || file.isDirectory();
    }

    /**
     * Compiles the rules of an Excel file.
     *
     * @return the compiled rules, or {@code null} when the file cannot be read and the failure is reported
     */
    private CompiledOpenClass compileWorkbook(String path, File file, long startTime) {
        String sourceFile = file.getName();
        try {
            new FileInputStream(file).close();
        } catch (Exception ex) {
            error(0, startTime, sourceFile, "Failed to read the excel file.", ex);
            return null;
        }

        RulesEngineFactory<?> engineFactory = new RulesEngineFactory<>(path + sourceFile);
        engineFactory.setExecutionMode(executionMode);
        return engineFactory.getCompiledOpenClass();
    }

    /**
     * Compiles the rules of a project folder, or of the project of a workspace folder that holds only projects.
     *
     * @return the compiled rules, or {@code null} when the compilation fails and the failure is reported
     */
    private CompiledOpenClass compileProject(File file, long startTime) {
        File[] filesInFolder = file.listFiles();
        boolean multiProject = filesInFolder != null && Arrays.stream(filesInFolder)
                .allMatch(File::isDirectory);
        try {
            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder =
                    new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
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
            CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();
            compiledOpenClass = validate(compiledOpenClass,
                    engineFactory.getProjectDescriptor(),
                    engineFactory.getRulesInstantiationStrategy());
            if (!compiledOpenClass.hasErrors() && engineFactory.newInstance() == null) {
                // To cover interface generation functionality
                throw new IllegalStateException("Failed to create an instance of the rules engine.");
            }
            return compiledOpenClass;
        } catch (Exception e) {
            error(0, startTime, file.getName(), "Compilation fails.", e);
            return null;
        }
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

    /**
     * Checks the compilation messages of one rules source and runs its tests. Every reported error is counted, and
     * the first one is preceded by the failure header of the source.
     */
    private final class SourceCheck {
        private final long startTime = System.nanoTime();
        private final String sourceFile;
        private int messagesCount;

        private SourceCheck(String sourceFile) {
            this.sourceFile = sourceFile;
        }

        /**
         * Compiles the rules source, checks its messages and runs its tests.
         *
         * @return {@code true} when the compilation fails or an error is reported
         */
        private boolean isFailed(String path, File testsDir, File file) {
            CompiledOpenClass compiledOpenClass = file.isDirectory() ? compileProject(file, startTime)
                    : compileWorkbook(path, file, startTime);
            if (compiledOpenClass == null) {
                // Nothing to check when the compilation fails
                return true;
            }

            boolean success = true;

            // Check messages
            File msgFile = new File(testsDir, sourceFile + ".msg.txt");
            if (msgFile.exists() && executionMode) {
                // Messages are not checked in the execution mode
                return false;
            }
            if (msgFile.exists()) {
                success = checkMessages(compiledOpenClass, msgFile);
            }

            // Check compilation
            if (success && compiledOpenClass.hasErrors()) {
                for (OpenLMessage msg : compiledOpenClass.getAllMessages()) {
                    reportError("   {}: {}    at {}", msg.getSeverity(), msg.getSummary(), msg.getSourceLocation());
                }
                success = false;
            }

            // Run tests
            if (success && !executionMode) {
                runTests(compiledOpenClass);
            }

            // Output
            if (messagesCount != 0) {
                return true;
            }
            ok(startTime, sourceFile);
            return false;
        }

        /**
         * Compares the messages of the compilation with the expected ones from the message file.
         *
         * @return {@code false} when an error is compiled, or a message is unexpected or missed
         */
        private boolean checkMessages(CompiledOpenClass compiledOpenClass, File msgFile) {
            boolean success = true;
            List<String> expectedMessages = readExpectedMessages(msgFile);

            Collection<OpenLMessage> unexpectedMessages = new LinkedHashSet<>();
            List<String> restMessages = new ArrayList<>(expectedMessages);
            for (OpenLMessage msg : compiledOpenClass.getAllMessages()) {
                String actual = msg.getSeverity() + ": " + msg.getSummary();
                if (msg.getSeverity().equals(Severity.ERROR)) {
                    success = false;
                }
                if (!removeExpectedMessage(restMessages, actual)) {
                    unexpectedMessages.add(msg);
                }
            }
            if (!unexpectedMessages.isEmpty()) {
                success = false;
                reportError("  UNEXPECTED messages:");
                for (OpenLMessage msg : unexpectedMessages) {
                    reportError("   {}: {}    at {}", msg.getSeverity(), msg.getSummary(), msg.getSourceLocation());
                }
            }
            if (!restMessages.isEmpty()) {
                success = false;
                reportError("  MISSED messages:");
                for (String msg : restMessages) {
                    reportError("   {}", msg);
                }
            }
            return success;
        }

        private List<String> readExpectedMessages(File msgFile) {
            List<String> expectedMessages = new ArrayList<>();
            try (var input = new FileInputStream(msgFile)) {
                String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                for (String message : content
                        .split("\\u000D\\u000A|[\\u000A\\u000B\\u000C\\u000D\\u0085\\u2028\\u2029]")) {
                    if (!StringUtils.isBlank(message)) {
                        expectedMessages.add(message.trim());
                    }
                }
            } catch (IOException exc) {
                reportError("Failed to read the message file '{}'.", msgFile, exc);
            }
            return expectedMessages;
        }

        private void runTests(CompiledOpenClass compiledOpenClass) {
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            Object target = openClass.newInstance(env);
            for (IOpenMethod method : openClass.getDeclaredMethods()) {
                if (method instanceof TestSuiteMethod) {
                    TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                    checkTestResults(res);
                }
            }
        }

        private void checkTestResults(TestUnitsResults res) {
            final int numberOfFailures = res.getNumberOfFailures();
            if (!allTestsMustFails) {
                if (numberOfFailures != 0) {
                    reportError("Failed test: {}  Errors #: {}", res.getName(), numberOfFailures);
                    List<ITestUnit> failed = res.getFilteredTestUnits(true, 3);
                    for (ITestUnit testcase : failed) {
                        reportError("\n   #{}  \n Actual: {} \n Expected: {}",
                                testcase.getTest().getId(),
                                testcase.getActualResult(),
                                testcase.getExpectedResult());
                    }
                }
            } else {
                if (numberOfFailures != res.getNumberOfTestUnits()) {
                    reportError("Unexpected test result: {}  Errors #: {}",
                            res.getName(),
                            res.getNumberOfTestUnits() - numberOfFailures);
                }
            }
        }

        private void reportError(String msg, Object... args) {
            error(messagesCount++, startTime, sourceFile, msg, args);
        }
    }

    /**
     * Removes the first expected message that the actual message contains.
     *
     * @return {@code true} when such a message is found
     */
    private static boolean removeExpectedMessage(List<String> restMessages, String actual) {
        Iterator<String> itr = restMessages.iterator();
        while (itr.hasNext()) {
            if (actual.contains(itr.next())) {
                itr.remove();
                return true;
            }
        }
        return false;
    }
}
