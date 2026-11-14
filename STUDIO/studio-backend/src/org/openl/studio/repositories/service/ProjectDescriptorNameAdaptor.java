package org.openl.studio.repositories.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.folder.FileAdaptor;

/**
 * Gives the {@code rules.xml} of a project the name the project is created or saved under.
 *
 * <p>The file is changed only where the name stands, so its comments, its layout and everything else the author
 * wrote stay as they are. A file that already holds the name is not changed at all.
 *
 * <p>A file without a name of its own, with a document type, or in an encoding other than UTF-8, is written anew
 * from what it describes.
 * A file that cannot be read is left as it is.
 */
public class ProjectDescriptorNameAdaptor implements FileAdaptor {

    /** The {@code name} of the project: the first one, which stands before the {@code name} of any module. */
    private static final Pattern NAME = Pattern.compile("<name>([^<]*)</name>");
    private static final Pattern ENCODING = Pattern.compile("^<\\?xml[^>]*encoding\\s*=\\s*[\"']([^\"']+)[\"']");

    private final String projectName;

    public ProjectDescriptorNameAdaptor(String projectName) {
        this.projectName = Objects.requireNonNull(projectName);
    }

    @Override
    public boolean accept(Path path) {
        var fileName = path.toString();
        if (fileName.charAt(0) == '/' || fileName.charAt(0) == '\\') {
            fileName = fileName.substring(1);
        }
        return ProjectDescriptor.FILE_NAME.equals(fileName);
    }

    @Override
    public InputStream apply(InputStream inputStream) throws IOException {
        var originalBytes = inputStream.readAllBytes();
        try {
            var projectDescriptor = ProjectDescriptor.read(new ByteArrayInputStream(originalBytes));
            if (projectName.equals(projectDescriptor.getName())) {
                return new ByteArrayInputStream(originalBytes);
            }
            var renamed = renamed(originalBytes);
            if (renamed != null) {
                return new ByteArrayInputStream(renamed);
            }
            projectDescriptor.setName(projectName);
            return new ByteArrayInputStream(projectDescriptor.toBytes());
        } catch (Exception e) {
            return new ByteArrayInputStream(originalBytes);
        }
    }

    /** Moves the matcher to its next match that does not stand in a comment, and tells whether there is one. */
    private static boolean findOutsideComments(Matcher matcher, String text) {
        while (matcher.find()) {
            var commentStart = text.lastIndexOf("<!--", matcher.start());
            if (commentStart < 0 || text.lastIndexOf("-->", matcher.start()) > commentStart) {
                return true;
            }
        }
        return false;
    }

    /** The file with the text of its project name replaced, or {@code null} when it cannot be done in place. */
    private byte @Nullable [] renamed(byte[] original) {
        var text = new String(original, StandardCharsets.UTF_8);
        var encoding = ENCODING.matcher(text.stripLeading());
        if (encoding.find() && !StandardCharsets.UTF_8.name().equalsIgnoreCase(encoding.group(1))) {
            return null;
        }
        // A document type can declare a name in an entity, which the project and its modules may share
        if (text.contains("<!DOCTYPE")) {
            return null;
        }
        var name = NAME.matcher(text);
        if (!findOutsideComments(name, text)) {
            return null;
        }
        var escaped = projectName.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        var bytes = (text.substring(0, name.start(1)) + escaped + text.substring(name.end(1)))
                .getBytes(StandardCharsets.UTF_8);
        // Only a file that reads back with the new name is kept, so a name of a module is never changed.
        try {
            return projectName.equals(ProjectDescriptor.read(new ByteArrayInputStream(bytes)).getName()) ? bytes : null;
        } catch (Exception e) {
            return null;
        }
    }
}
