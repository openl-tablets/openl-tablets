package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ValidationException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.files.ProjectFilesService.UploadedFile;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * A project descriptor written through the files API is validated before it reaches the repository,
 * whichever way it is written. Content that is not a descriptor is written as it is.
 */
class ProjectDescriptorWriteValidationTest {

    private static final String INVALID_PROCESSOR = """
            <project>
                <name>Project1</name>
                <properties-file-name-processor>aaa</properties-file-name-processor>
            </project>""";

    private static final String VALID_DESCRIPTOR = """
            <project>
                <name>Project1</name>
                <properties-file-name-pattern>%lob%-%state%</properties-file-name-pattern>
            </project>""";

    private Repository repository;
    private RulesProject project;
    private ProjectFileRoot root;
    private ProjectFilesServiceImpl service;

    @BeforeEach
    void init(@TempDir Path workspace) throws IOException {
        repository = mock(Repository.class);
        project = mock(RulesProject.class);
        when(project.getRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        when(project.isOpened()).thenReturn(true);
        // The project is checked out, so its classpath - and the processor class on it - can be read.
        var localRepository = mock(LocalRepository.class);
        when(localRepository.getRoot()).thenReturn(workspace);
        when(project.getLocalRepository()).thenReturn(localRepository);
        when(project.getLocalFolderName()).thenReturn("Project1");
        Files.createDirectory(workspace.resolve("Project1"));

        AclProjectsHelper acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        ProjectStateValidator stateValidator = mock(ProjectStateValidator.class);
        when(stateValidator.canModify(project)).thenReturn(true);
        root = new ProjectFileRoot(project, acl, stateValidator, mock(ProjectFileLookupService.class),
                () -> new UserInfo("user1"));

        service = new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()));
    }

    @Test
    void updateOfDescriptorWithUnknownProcessorIsRejected() throws Exception {
        projectWithDescriptor();

        var ex = assertThrows(ValidationException.class,
                () -> service.updateResource(root, ProjectDescriptor.FILE_NAME, content(INVALID_PROCESSOR)));

        var error = ex.getBindingResult().getFieldError("propertiesFileNameProcessor");
        assertNotNull(error);
        assertEquals("file.descriptor.processor.invalid.message", error.getCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void updateOfValidDescriptorIsWritten() throws Exception {
        projectWithDescriptor();

        assertDoesNotThrow(
                () -> service.updateResource(root, ProjectDescriptor.FILE_NAME, content(VALID_DESCRIPTOR)));

        verify(repository).save(any(FileData.class), any(InputStream.class));
    }

    @Test
    void creationOfDescriptorWithUnknownProcessorIsRejected() throws Exception {
        emptyProject();

        assertThrows(ValidationException.class,
                () -> service.createResource(root, ProjectDescriptor.FILE_NAME, content(INVALID_PROCESSOR), false));

        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    /**
     * An upload brings its own files with it - the libraries a descriptor names among them - so its
     * descriptor is not checked against the working copy the upload is about to replace.
     */
    @Test
    void uploadedDescriptorIsNotValidated() {
        emptyProject();

        assertDoesNotThrow(() -> service.uploadFiles(root, "",
                List.of(new UploadedFile(ProjectDescriptor.FILE_NAME,
                        INVALID_PROCESSOR.getBytes(StandardCharsets.UTF_8))),
                ConflictPolicy.OVERWRITE));
    }

    /**
     * The path arrives as the request wrote it: a trailing slash still names the descriptor, and must
     * not make the check skip.
     */
    @Test
    void descriptorPathWithTrailingSlashIsValidated() throws Exception {
        projectWithDescriptor();

        assertThrows(ValidationException.class,
                () -> service.updateResource(root, ProjectDescriptor.FILE_NAME + "/", content(INVALID_PROCESSOR)));

        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    /**
     * Content too large to be a descriptor is refused instead of being written unchecked - padding a
     * descriptor past the cap must not smuggle unusable settings in.
     */
    @Test
    void oversizedDescriptorIsRefused() throws Exception {
        projectWithDescriptor();
        var padding = " ".repeat(16 * 1024 * 1024);

        var ex = assertThrows(BadRequestException.class, () -> service.updateResource(root,
                ProjectDescriptor.FILE_NAME, content(INVALID_PROCESSOR + "<!--" + padding + "-->")));

        assertEquals("openl.error.400.file.descriptor.too-large.message", ex.getErrorCode());
        verify(repository, never()).save(any(FileData.class), any(InputStream.class));
    }

    /**
     * A rejected write must leave no trace: an opened project is reserved for editing only once the
     * content is known to be writable.
     */
    @Test
    void rejectedDescriptorLeavesTheProjectUnlocked() throws Exception {
        projectWithDescriptor();

        assertThrows(ValidationException.class,
                () -> service.updateResource(root, ProjectDescriptor.FILE_NAME, content(INVALID_PROCESSOR)));

        verify(project, never()).tryLockOrThrow();
    }

    /**
     * The stream the caller opened is consumed in full by the validation, so it is closed there instead
     * of by the write that no longer receives it.
     */
    @Test
    void validatedDescriptorStreamIsClosed() throws Exception {
        projectWithDescriptor();
        var closed = new AtomicBoolean();
        var stream = new FilterInputStream(content(VALID_DESCRIPTOR)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };

        service.updateResource(root, ProjectDescriptor.FILE_NAME, stream);

        assertTrue(closed.get());
    }

    /**
     * A descriptor is a file like any other outside a project mount: a repository mount holds many
     * projects and writes to it are not validated as one project's descriptor.
     */
    @Test
    void descriptorOfRepositoryMountIsNotValidated() throws Exception {
        FileRoot repoMount = mock(FileRoot.class);
        var tree = new AProjectFolder(new HashMap<>(), mock(AProject.class), repository, "");
        when(repoMount.writeFolder()).thenReturn(tree);
        when(repoMount.readFolder(null)).thenReturn(tree);
        when(repository.save(any(FileData.class), any(InputStream.class))).thenReturn(new FileData());

        assertDoesNotThrow(() -> service.createResource(repoMount, ProjectDescriptor.FILE_NAME,
                content(INVALID_PROCESSOR), false));
    }

    /**
     * A descriptor that is not well-formed cannot be validated; it is written as it is, so a broken
     * file can be replaced by a fixed one.
     */
    @Test
    void brokenDescriptorIsWrittenAsItIs() throws Exception {
        projectWithDescriptor();

        assertDoesNotThrow(() -> service.updateResource(root, ProjectDescriptor.FILE_NAME, content("<project")));

        verify(repository).save(any(FileData.class), any(InputStream.class));
    }

    /**
     * A descriptor rewritten for another reason - a module registered, a project migrated - keeps the
     * settings the project already stores, so it is written even when those settings are unusable.
     */
    @Test
    void settingsTheProjectAlreadyStoresAreNotRejected() throws Exception {
        projectWithDescriptor(INVALID_PROCESSOR);

        assertDoesNotThrow(() -> service.updateResource(root, ProjectDescriptor.FILE_NAME,
                content(INVALID_PROCESSOR.replace("</project>", "    <comment>Module added</comment>\n</project>"))));

        verify(repository).save(any(FileData.class), any(InputStream.class));
    }

    private static InputStream content(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private void projectWithDescriptor() throws IOException {
        projectWithDescriptor("<project><name>Project1</name></project>");
    }

    private void projectWithDescriptor(String stored) throws IOException {
        var fileData = new FileData();
        fileData.setName("Project1/" + ProjectDescriptor.FILE_NAME);
        var resource = new AProjectResource(project, repository, fileData);
        when(project.getArtefacts()).thenReturn(List.of(resource));
        when(repository.read("Project1/" + ProjectDescriptor.FILE_NAME))
                .thenReturn(new FileItem(fileData, content(stored)));
    }

    private void emptyProject() {
        when(project.getArtefacts()).thenReturn(List.of());
    }
}
