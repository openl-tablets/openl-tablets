package org.openl.rules.ui;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicReference;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Sessions whose real methods run on a model the test chooses.
 *
 * @author Yury Molchan
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class WebStudioMocks {

    /**
     * A session whose real methods run on the given model.
     *
     * <p>The mock skips the constructor, so the holders of what a write leaves waiting, and the lock of the projects,
     * are supplied here.
     */
    static WebStudio studio(ProjectModel model) {
        var studio = mock(WebStudio.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(studio, "projectsLock", new Object());
        ReflectionTestUtils.setField(studio, "rewrittenModule", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "moduleToVerify", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "announcedWrite", new AtomicReference<>());
        ReflectionTestUtils.setField(studio, "model", model);
        return studio;
    }
}
