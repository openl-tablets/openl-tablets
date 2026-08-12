package org.openl.studio.projects.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class ChangeOriginFilterTest {

    @Mock
    private ChangeOriginResolver changeOrigin;

    @InjectMocks
    private ChangeOriginFilter filter;

    private void handle(String method, String uri) throws Exception {
        filter.doFilter(new MockHttpServletRequest(method, uri), new MockHttpServletResponse(), new MockFilterChain());
    }

    @Test
    void marks_the_client_of_a_change_as_a_recent_writer() throws Exception {
        handle("PATCH", "/web/projects/p1");

        // Twice: when the request starts and when it ends, so a slow write stays attributable while
        // the disk events and the index rebuild it causes are still arriving.
        verify(changeOrigin, times(2)).remember(any());
    }

    @Test
    void marks_a_change_made_through_the_rest_api_or_the_legacy_pages_too() throws Exception {
        // Neither goes through the Spring dispatcher's interceptors, and both change the workspace
        // of a user whose browser tab must not read their changes as its own echo.
        handle("POST", "/rest/projects/p1/files/a.txt");
        handle("POST", "/faces/pages/modules/index.xhtml");

        verify(changeOrigin, times(4)).remember(any());
    }

    @Test
    void a_read_changes_nothing_and_marks_nobody() throws Exception {
        handle("GET", "/web/projects");

        verify(changeOrigin, never()).remember(any());
    }
}
