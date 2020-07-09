package org.openl.rules.project.resolving;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.openl.rules.project.model.ProjectDescriptor;

public final class ProjectResource {
    private final URL url;

    private final ProjectDescriptor projectDescriptor;

    ProjectResource(ProjectDescriptor projectDescriptor, URL url) {
        this.url = Objects.requireNonNull(url, "url cannot be null");
        this.projectDescriptor = Objects.requireNonNull(projectDescriptor, "projectDescriptor cannot be null");
    }

    public URL getUrl() {
        return url;
    }

    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    public String getFile() {
        try {
            return new File(URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name())).getPath();
        } catch (UnsupportedEncodingException ignored) {
            return new File(url.getFile()).getPath();
        }
    }
}
