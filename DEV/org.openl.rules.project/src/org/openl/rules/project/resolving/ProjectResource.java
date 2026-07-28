package org.openl.rules.project.resolving;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.Getter;

import org.openl.rules.project.model.ProjectDescriptor;

public final class ProjectResource {
    @Getter
    private final URL url;

    @Getter
    private final ProjectDescriptor projectDescriptor;

    ProjectResource(ProjectDescriptor projectDescriptor, URL url) {
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.projectDescriptor = Objects.requireNonNull(projectDescriptor, "projectDescriptor cannot be null");
    }

    public String getFile() {
        return new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8)).getPath();
    }
}
