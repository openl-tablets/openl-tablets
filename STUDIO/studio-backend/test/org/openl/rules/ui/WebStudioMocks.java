package org.openl.rules.ui;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sessions whose real methods run on a model the test chooses.
 *
 * @author Yury Molchan
 */
final class WebStudioMocks {

    private WebStudioMocks() {
    }

    /**
     * A session whose real methods run on the given model.
     *
     * <p>The mock skips the constructor, so the holders of what a write leaves waiting are supplied here.
     */
    static WebStudio studio(ProjectModel model) {
        var studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(studio, "rewrittenModule", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "moduleToVerify", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "announcedWrite", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "model", model);
        return studio;
    }
}
