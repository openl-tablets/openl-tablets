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
import org.openl.rules.workspace.dtr.DesignTimeRepository;
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
    void anIdNoDesignProjectHasAnswersNotFound() {
        when(designTimeRepository.getProjectById(any())).thenReturn(Optional.empty());

        var ex = assertThrows(NotFoundException.class, () -> converter.convert("ZGVzaWduOkJhbms="));

        assertEquals("openl.error.404.project.identifier.message", ex.getErrorCode());
    }
}
