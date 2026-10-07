package org.openl.studio.openapi.app000;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.projection.test.ProjectTestView;

/**
 * Test controller covering the response shapes the field-projection OpenAPI customizer must handle.
 *
 * <p>Projectable -- should advertise the {@code fields} parameter:
 * <ul>
 *     <li>{@code GET /projects/{id}} -- single DTO;</li>
 *     <li>{@code GET /projects} -- list of DTOs;</li>
 *     <li>{@code GET /projects/page} -- paginated DTOs.</li>
 * </ul>
 *
 * <p>Non-projectable -- no {@code fields} parameter expected:
 * <ul>
 *     <li>{@code GET /projects/{id}/name} -- returns a {@link String}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/projects")
public class ProjectController {

    @GetMapping("/{id}")
    public ProjectTestView get(@PathVariable String id) {
        return null;
    }

    @GetMapping
    public List<ProjectTestView> list() {
        return null;
    }

    @GetMapping("/page")
    public PageResponse<ProjectTestView> page() {
        return null;
    }

    @GetMapping("/{id}/name")
    public String name(@PathVariable String id) {
        return null;
    }
}
