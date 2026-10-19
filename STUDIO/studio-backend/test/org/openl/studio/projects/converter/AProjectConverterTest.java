package org.openl.studio.projects.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.ProjectIds;
import org.openl.rules.workspace.StaleIds;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.studio.common.exception.GoneException;
import org.openl.studio.common.exception.NotFoundException;

class AProjectConverterTest {

    private final DesignTimeRepository designTimeRepository = mock(DesignTimeRepository.class);
    private final AProjectConverter converter = new AProjectConverter(designTimeRepository);

    @Test
    void anIdNamesTheDesignProjectOfItsFolder() {
        var id = ProjectIds.of("design", "DESIGN/rules/Bank");
        var project = mock(AProject.class);
        when(designTimeRepository.getProjectById(id)).thenReturn(Optional.of(project));

        assertSame(project, converter.convert(id.value()));
    }

    @Test
    void anIdOfAMovedProjectNamesTheProjectOfItsNewFolder() {
        var id = ProjectIds.of("design", "DESIGN/rules/Bank");
        var current = ProjectIds.of("design", "DESIGN/rules/archive/Bank");
        var project = mock(AProject.class);
        when(designTimeRepository.getProjectById(id)).thenReturn(Optional.empty());
        when(designTimeRepository.getProjectIdChange(id)).thenReturn(Optional.of(new StaleIds.Moved<>(current)));
        when(designTimeRepository.getProjectById(current)).thenReturn(Optional.of(project));

        assertSame(project, converter.convert(id.value()));
    }

    @Test
    void anIdOfADeletedProjectAnswersGone() {
        var id = ProjectIds.of("design", "DESIGN/rules/Bank");
        when(designTimeRepository.getProjectById(id)).thenReturn(Optional.empty());
        when(designTimeRepository.getProjectIdChange(id)).thenReturn(Optional.of(new StaleIds.Deleted<>()));

        var text = id.value();
        var ex = assertThrows(GoneException.class, () -> converter.convert(text));

        assertEquals("openl.error.410.project.deleted.message", ex.getErrorCode());
    }

    @Test
    void anIdNoDesignProjectHasAnswersNotFound() {
        when(designTimeRepository.getProjectById(any())).thenReturn(Optional.empty());

        var ex = assertThrows(NotFoundException.class, () -> converter.convert("ZGVzaWduOkJhbms="));

        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }
}
