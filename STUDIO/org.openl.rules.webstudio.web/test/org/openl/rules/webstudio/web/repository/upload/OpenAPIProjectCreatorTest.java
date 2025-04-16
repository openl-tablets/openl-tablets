package org.openl.rules.webstudio.web.repository.upload;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.richfaces.model.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.LockEngine;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.validation.openapi.OpenApiProjectValidator;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.repository.file.FileSystemRepository;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.StringUtils;

public class OpenAPIProjectCreatorTest {

    private final Logger log = LoggerFactory.getLogger("OpenAPI Project Creator Test");

    private static final Set<String> OPENAPI_EXTS = Stream.of(".json", ".yaml", ".yml").collect(Collectors.toSet());
    private static final String REPO_ID = UUID.randomUUID().toString();
    private static final String DEFAULT_COMMENT = "";
    private static final String MOCK_MODEL_NAME = "Models";
    private static final String MOCK_MODEL_PATH = String.format("rules/%s.xlsx", MOCK_MODEL_NAME);
    private static final String MOCK_ALGORITHM_NAME = "Algorithms";
    private static final String MOCK_ALGORITHM_PATH = String.format("rules/%s.xlsx", MOCK_ALGORITHM_NAME);
    public static final String DIR = "test-resources/openapi/functionality";
    public static final String OPENAPI_OUT = System.getProperty("openapi.output.dir");

    private UserWorkspace userWorkspaceMock;
    private FileSystemRepository tempRepo;
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @BeforeEach
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @BeforeEach
    public void setUp() {
        tempRepo = new FileSystemRepository();
        tempRepo.setUri(OPENAPI_OUT);
        tempRepo.initialize();

        userWorkspaceMock = mock(UserWorkspace.class);
        DesignTimeRepository designTimeRepoMock = mock(DesignTimeRepository.class);

        when(designTimeRepoMock.getRulesLocation()).thenReturn("");
        when(designTimeRepoMock.getRepository(REPO_ID)).thenReturn(tempRepo);

        when(userWorkspaceMock.getDesignTimeRepository()).thenReturn(designTimeRepoMock);
        when(userWorkspaceMock.getUser()).thenReturn(new WorkspaceUserImpl("USER_MOCK",
                (username) -> new UserInfo("USER_MOCK", "USER_MOCK@email", "USER MOCK")));
        LockEngine lockEngine = mock(LockEngine.class);
        when(lockEngine.tryLock(nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(true);
        when(lockEngine.getLockInfo(nullable(String.class), nullable(String.class), nullable(String.class))).thenReturn(LockInfo.NO_LOCK);

        when(userWorkspaceMock.getProjectsLockEngine()).thenAnswer((x) -> lockEngine);
    }

    @Test
    public void testAll() {
        assertFalse(run(DIR), "Test is failed.");
    }

    protected CompiledOpenClass validate(CompiledOpenClass compiledOpenClass,
                                         ProjectDescriptor projectDescriptor,
                                         RulesInstantiationStrategy rulesInstantiationStrategy) {
        try {
            OpenApiProjectValidator openApiProjectValidator = new OpenApiProjectValidator();
            return openApiProjectValidator.validate(projectDescriptor, rulesInstantiationStrategy);
        } catch (RulesInstantiationException e) {
            return compiledOpenClass;
        }
    }

    public boolean run(String path) {
        log.info(">>> Compiling rules and running tests from the directory '{}'...", path);

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
            if (file.isFile() && OPENAPI_EXTS.stream().anyMatch(sourceFile::endsWith)) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    Files.copy(file.toPath(), out);

                    UploadedFile uploadedFile = mock(UploadedFile.class);
                    when(uploadedFile.getName()).thenReturn(sourceFile);
                    when(uploadedFile.getSize()).thenReturn((long) out.size());
                    when(uploadedFile.getInputStream()).thenReturn(new ByteArrayInputStream(out.toByteArray()));

                    OpenAPIProjectCreator projectCreator = null;
                    try {
                        projectCreator = new OpenAPIProjectCreator(new ProjectFile(uploadedFile),
                                REPO_ID,
                                sourceFile,
                                sourceFile,
                                userWorkspaceMock,
                                DEFAULT_COMMENT,
                                MOCK_MODEL_PATH,
                                MOCK_ALGORITHM_PATH,
                                MOCK_MODEL_NAME,
                                MOCK_ALGORITHM_NAME, 
                                Collections.emptyMap());
                        projectCreator.createRulesProject();
                    } finally {
                        Optional.ofNullable(projectCreator).ifPresent(OpenAPIProjectCreator::destroy);
                    }
                    Path projectFolderPath = Paths.get(OPENAPI_OUT, sourceFile);
                    SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> engineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>();
                    engineFactoryBuilder.setExecutionMode(false);
                    engineFactoryBuilder.setProject(projectFolderPath.toAbsolutePath().toFile().getPath());
                    SimpleProjectEngineFactory<Object> engineFactory = engineFactoryBuilder.build();
                    compiledOpenClass = engineFactory.getCompiledOpenClass();
                    compiledOpenClass = validate(compiledOpenClass,
                            engineFactory.getProjectDescriptor(),
                            engineFactory.getRulesInstantiationStrategy());
                } catch (Exception e) {
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
        final long ms = (System.nanoTime() - startTime) / 1000000;
        log.info("\u001B[1;32mSUCCESS\u001B[0m - in [\u001B[2;36m{}\u001B[0m] ({} ms)", sourceFile, ms);
    }

    private void error(int count, long startTime, String sourceFile, String msg, Object... args) {
        if (count == 0) {
            final long ms = (System.nanoTime() - startTime) / 1000000;
            log.error("\u001B[1;31mFAILURE\u001B[0m - in [\u001B[2;36m{}\u001B[0m] ({} ms)", sourceFile, ms);
        }
        log.error(msg, args);
    }

}