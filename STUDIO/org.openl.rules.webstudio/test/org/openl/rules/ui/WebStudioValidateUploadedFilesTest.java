package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.webstudio.util.NameChecker;
import org.openl.rules.webstudio.web.repository.upload.ZipProjectDescriptorExtractor;

/**
 * Verifies that {@link WebStudio#validateUploadedFiles} tolerates a missing project name in the uploaded
 * {@code rules.xml}.
 *
 * <p>The name is optional and defaults to the folder name, so a blank uploaded name is not a rename and
 * must neither raise a {@link NullPointerException} nor be reported as an error.
 *
 * @author Yury Molchan
 */
class WebStudioValidateUploadedFilesTest {

    private static ProjectDescriptor descriptor(String name) {
        var pd = new ProjectDescriptor();
        pd.setName(name);
        return pd;
    }

    @Test
    void blankUploadedNameIsNotTreatedAsRename() throws Exception {
        WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        try (MockedStatic<ZipProjectDescriptorExtractor> extractor = mockStatic(ZipProjectDescriptorExtractor.class)) {
            extractor.when(() -> ZipProjectDescriptorExtractor.getProjectDescriptorOrThrow(any(), any(), any()))
                    .thenReturn(descriptor(null));
            assertNull(studio.validateUploadedFiles(null, null, descriptor("Existing"), null));
        }
    }

    @Test
    void differingUploadedNameIsValidated() throws Exception {
        WebStudio studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        try (MockedStatic<ZipProjectDescriptorExtractor> extractor = mockStatic(ZipProjectDescriptorExtractor.class)) {
            extractor.when(() -> ZipProjectDescriptorExtractor.getProjectDescriptorOrThrow(any(), any(), any()))
                    .thenReturn(descriptor("bad/name"));
            assertEquals(NameChecker.BAD_PROJECT_NAME_MSG,
                    studio.validateUploadedFiles(null, null, descriptor("Existing"), null));
        }
    }
}
