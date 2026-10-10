package org.openl.rules.webstudio.web.repository.upload;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.common.exception.BadRequestException;

/**
 * How a project built from uploaded files refuses a file it cannot hold.
 *
 * @author Yury Molchan
 */
class RulesProjectBuilderTest {

    private final RulesProjectBuilder builder = builder();

    @AfterEach
    void cancel() {
        builder.cancel();
    }

    @Test
    void refusesAFileNameAProjectCannotHave() {
        var content = InputStream.nullInputStream();

        var refused = assertThrows(BadRequestException.class, () -> builder.addFile("Rating?.xlsx", content));

        assertEquals("openl.error.400.project.file.name.invalid.message", refused.getErrorCode());
        assertArrayEquals(new Object[]{"Rating?.xlsx", NameChecker.FORBIDDEN_CHARS_STRING}, refused.getArgs());
    }

    @Test
    void refusesAFolderNameAProjectCannotHave() {
        var content = InputStream.nullInputStream();

        var refused = assertThrows(BadRequestException.class, () -> builder.addFile("rates*/Rating.xlsx", content));

        assertArrayEquals(new Object[]{"rates*", NameChecker.FORBIDDEN_CHARS_STRING}, refused.getArgs());
    }

    @Test
    void answersARefusedFileWithTheReasonNotAsAFailedCreation() {
        var refusal = new BadRequestException("project.file.name.invalid.message");
        var creator = new AProjectCreator("Project", "", mock(UserWorkspace.class), Map.of()) {
            @Override
            protected RulesProjectBuilder getProjectBuilder() {
                throw refusal;
            }

            @Override
            public void destroy() {
                // nothing was uploaded
            }
        };

        assertSame(refusal, assertThrows(BadRequestException.class, creator::createRulesProject));
    }

    private static RulesProjectBuilder builder() {
        var designTimeRepository = mock(DesignTimeRepository.class);
        when(designTimeRepository.getRulesLocation()).thenReturn("DESIGN/rules/");
        var workspace = mock(UserWorkspace.class);
        when(workspace.getDesignTimeRepository()).thenReturn(designTimeRepository);
        when(workspace.getUser()).thenReturn(new WorkspaceUserImpl("jdoe", id -> null));
        var designRepository = mock(Repository.class);
        when(designRepository.getId()).thenReturn("design");
        when(designRepository.supports()).thenReturn(new FeaturesBuilder(designRepository).build());
        return new RulesProjectBuilder(workspace, designRepository, "Project", "", "Create Project");
    }
}
