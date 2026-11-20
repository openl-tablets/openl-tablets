package org.openl.studio.dependencies.rest.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.studio.dependencies.model.MavenProjectSummary;
import org.openl.studio.dependencies.service.MavenProjectViewer;

/**
 * The OpenL projects downloaded from the Maven repository that a {@code rules.xml} of the workspace names by Maven
 * coordinates.
 *
 * <p>Such a project is read through the Projects API, by the id {@code maven:groupId:artifactId:version}.
 *
 * @author Yury Molchan
 */
@RestController
@RequestMapping(value = "/maven", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dependencies: Maven", description = "Read-only OpenL projects downloaded from the Maven repository")
@RequiredArgsConstructor
public class MavenDependencyController {

    private final MavenProjectViewer viewer;

    @GetMapping
    @Operation(summary = "dependencies.maven.list.summary", description = "dependencies.maven.list.desc")
    public List<MavenProjectSummary> getProjects() {
        return viewer.projects();
    }
}
