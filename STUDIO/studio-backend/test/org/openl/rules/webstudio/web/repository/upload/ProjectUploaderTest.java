package org.openl.rules.webstudio.web.repository.upload;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.project.ExcelFilesProjectCreator;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.repository.upload.zip.ZipCharsetDetector;
import org.openl.rules.workspace.filter.PathFilter;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclRole;
import org.openl.security.acl.repository.RepositoryAclService;

class ProjectUploaderTest {

    @Test
    void successfulWriteDoesNotResolveTheCreatedProjectWithoutItsBranch() throws Exception {
        var repository = mock(Repository.class);
        var workspace = mock(UserWorkspace.class);
        var acl = mock(RepositoryAclService.class);
        var rulesProject = mock(RulesProject.class);
        @SuppressWarnings("unchecked")
        var finalizeProject = (Consumer<RulesProject>) mock(Consumer.class);
        var awaitVisibility = mock(Runnable.class);
        when(acl.createAcl(rulesProject, List.of(AclRole.CONTRIBUTOR.getCumulativePermission()), true)).thenReturn(true);

        try (var creators = mockConstruction(ExcelFilesProjectCreator.class, (creator, context) -> {
            when(creator.createRulesProject()).thenReturn(rulesProject);
            when(creator.getCreatedProjectName()).thenReturn("Project");
        })) {
            var uploader = new ProjectUploader(
                    repository,
                    List.of(new ProjectFile("rules.xlsx", InputStream.nullInputStream())),
                    "Project",
                    "",
                    workspace,
                    acl,
                    "Create Project",
                    mock(PathFilter.class),
                    mock(ZipCharsetDetector.class),
                    "rules/Models.xlsx",
                    "rules/Algorithms.xlsx",
                    "Models",
                    "Algorithms",
                    Map.of(),
                    finalizeProject,
                    awaitVisibility);

            assertSame(rulesProject, uploader.uploadProject());

            verify(finalizeProject).accept(rulesProject);
            verify(awaitVisibility).run();
            verifyNoInteractions(workspace);
            verify(creators.constructed().getFirst()).destroy();
        }
    }
}
