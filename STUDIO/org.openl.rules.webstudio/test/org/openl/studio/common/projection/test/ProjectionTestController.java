package org.openl.studio.common.projection.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.projection.NoFieldProjection;

@RestController
@RequestMapping("/projection-test")
public class ProjectionTestController {

    private static ProjectTestView project(String id) {
        return new ProjectTestView(id, "name-" + id, "OPENED", "secret-" + id, "writeOnly-" + id,
                new UserTestView("login-" + id, id + "@example.com"),
                List.of(new UserTestView("member-" + id, "member-" + id + "@example.com")));
    }

    @GetMapping("/single")
    public ProjectTestView single() {
        return project("1");
    }

    @GetMapping("/list")
    public List<ProjectTestView> list() {
        return List.of(project("1"), project("2"));
    }

    @GetMapping("/list-empty")
    public List<ProjectTestView> listEmpty() {
        return List.of();
    }

    @GetMapping("/set")
    public Set<ProjectTestView> set() {
        return new LinkedHashSet<>(List.of(project("1"), project("2")));
    }

    @GetMapping("/page")
    public PageResponse<ProjectTestView> page() {
        return new PageResponse<>(List.of(project("1"), project("2")), 0, 10, 2L);
    }

    @GetMapping("/page-subclass")
    public SubPage pageSubclass() {
        return new SubPage(List.of(project("1"), project("2")), 0, 10, 2L);
    }

    /** A concrete {@link PageResponse} subclass, mirroring production wrappers such as ProjectsPageResponse. */
    static class SubPage extends PageResponse<ProjectTestView> {
        SubPage(java.util.Collection<ProjectTestView> content, int pageNumber, int pageSize, Long total) {
            super(content, pageNumber, pageSize, total);
        }
    }

    @GetMapping("/page-with-summary")
    public SummaryPage pageWithSummary() {
        return new SummaryPage(List.of(project("1"), project("2")), 0, 10, 2L, new TestSummary(3, 4));
    }

    /** A page carrying a {@link NoFieldProjection} summary alongside its projectable content. */
    static class SummaryPage extends PageResponse<ProjectTestView> {
        private final TestSummary summary;

        SummaryPage(java.util.Collection<ProjectTestView> content, int pageNumber, int pageSize, Long total,
                    TestSummary summary) {
            super(content, pageNumber, pageSize, total);
            this.summary = summary;
        }

        public TestSummary getSummary() {
            return summary;
        }
    }

    @NoFieldProjection
    record TestSummary(long alpha, long beta) {
    }

    @GetMapping(value = "/text", produces = MediaType.TEXT_PLAIN_VALUE)
    public String text() {
        return "plain text";
    }

    @GetMapping(value = "/bytes", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] bytes() {
        return "binary".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
