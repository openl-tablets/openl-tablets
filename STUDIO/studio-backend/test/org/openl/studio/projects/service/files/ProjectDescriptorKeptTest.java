package org.openl.studio.projects.service.files;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.IgnoredPaths;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.tables.TableFormatting;
import org.openl.studio.projects.service.tables.UploadFormatting;
import org.openl.studio.projects.validator.ProjectStateValidator;

/**
 * The {@code rules.xml} at the root of a project in a repository with folders is the only thing that makes the folder a
 * project while folders are not found as projects by their Excel files, so it can be neither deleted nor moved. A
 * repository that keeps projects as archives, or the Excel detection, lets it go.
 *
 * @author Yury Molchan
 */
class ProjectDescriptorKeptTest {

    private final Repository repository = mock(Repository.class);
    private final RulesProject project = mock(RulesProject.class);
    private ProjectFileRoot root;

    private ProjectFilesServiceImpl service(boolean folders, boolean detectByExcel) {
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).setFolders(folders).build());
        when(project.getRepository()).thenReturn(repository);
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getFolderPath()).thenReturn("Project1");
        when(project.isOpened()).thenReturn(true);
        var descriptor = new FileData();
        descriptor.setName("Project1/rules.xml");
        var data = new FileData();
        data.setName("Project1/data.txt");
        when(project.getArtefacts()).thenReturn(List.of(new AProjectResource(project, repository, descriptor),
                new AProjectResource(project, repository, data)));

        var acl = mock(AclProjectsHelper.class);
        when(acl.hasPermission(any(AProject.class), any())).thenReturn(true);
        when(acl.hasPermission(any(AProjectArtefact.class), any())).thenReturn(true);
        root = new ProjectFileRoot(project, acl, mock(ProjectStateValidator.class), mock(ProjectFileLookupService.class),
                () -> new UserInfo("user1"), mock(DesignTimeRepository.class));
        return new ProjectFilesServiceImpl(acl, mock(FileNodeMapper.class), mock(FileSearchSupport.class),
                new FileArchiveSupport(acl), mock(ProjectDescriptorCleaner.class),
                new BeanValidationProvider(List.of()), new UploadFormatting(mock(TableFormatting.class)),
                IgnoredPaths.of(null), detectByExcel);
    }

    @Test
    void theDescriptorCannotBeDeleted() throws Exception {
        var service = service(true, false);

        var error = assertThrows(ConflictException.class, () -> service.deleteResource(root, "rules.xml", null));

        assertEquals("openl.error.409.file.descriptor.required.message", error.getErrorCode());
        verify(repository, never()).delete(any(FileData.class));
    }

    @ParameterizedTest(name = "to {0}")
    @CsvSource({"old/rules.xml", "rules-old.xml"})
    void theDescriptorCannotBeMoved(String destination) throws Exception {
        var service = service(true, false);

        var error = assertThrows(ConflictException.class, () -> service.moveResource(root, "rules.xml", destination));

        assertEquals("openl.error.409.file.descriptor.required.message", error.getErrorCode());
        verify(repository, never()).delete(any(FileData.class));
    }

    @ParameterizedTest(name = "folders {0}, Excel detection {1}")
    @CsvSource({"false, false", "true, true"})
    void theDescriptorCanBeDeletedWhenItDoesNotMakeTheProject(boolean folders, boolean detectByExcel) {
        var service = service(folders, detectByExcel);

        assertDoesNotThrow(() -> service.deleteResource(root, "rules.xml", null));
    }

    @Test
    void anotherFileCanBeDeleted() {
        var service = service(true, false);

        assertDoesNotThrow(() -> service.deleteResource(root, "data.txt", null));
    }
}
