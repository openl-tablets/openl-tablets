package org.openl.rules.webstudio.dependencies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.util.formatters.FileNameFormatter;

/**
 * Reads the {@code pom.xml} of a project, whose dependencies OpenL Studio takes into account.
 *
 * <p>A parent of a build of several modules lies outside the project, such as {@code ../pom.xml}. The copy of an opened
 * project holds the project alone, so the POMs the {@code pom.xml} names by a relative path are read from where the
 * project is kept: its design repository, at the head of its branch, as a build of the branch reads them, or at the
 * revision opened when it is an older one. A repository keeping each project as one archive holds no POM beside it.
 *
 * <p>A POM on disk is read only inside the folder it is given, so a relative path cannot reach another file of the
 * server, such as a project of another user.
 *
 * @author Yury Molchan
 */
public final class ProjectPoms {

    private ProjectPoms() {
    }

    /**
     * The {@code pom.xml} the folder holds on disk, which finds the POMs it names by a relative path on disk too, but
     * only inside the root.
     *
     * @param folder the folder of the project
     * @param root   the folder the POMs it names are read in, such as the folder itself
     * @return the {@code pom.xml}, or {@code null} when the folder holds none
     */
    public static @Nullable ProjectPom inFolder(Path folder, Path root) throws IOException {
        var pom = folder.toAbsolutePath().normalize().resolve(ProjectPom.FILE_NAME);
        if (!Files.isRegularFile(pom)) {
            return null;
        }
        var within = root.toAbsolutePath().normalize();
        return new ProjectPom(Files.readString(pom), FileNameFormatter.normalizePath(pom), "file", path -> {
            var file = Path.of(path).normalize();
            return file.startsWith(within) && Files.isRegularFile(file) ? Files.readString(file) : null;
        });
    }

    /**
     * The {@code pom.xml} of the project, whose POMs named by a relative path, such as a parent of a build of several
     * modules, are read from its design repository.
     *
     * <p>The text is the one the folder of the project holds on disk, with the changes not saved yet, or else the one
     * of the design repository. The repository is read at the head of the branch of the project, as a build of the
     * branch reads the parents, or at the revision opened when it is an older one. A project kept as one archive, as
     * in a database or AWS S3, has its {@code pom.xml} read from the archive and no POM beside it; a project of no
     * design repository finds the POMs inside its folder on disk.
     *
     * @param project the project
     * @param folder  the folder of the project on disk, or {@code null} to read the {@code pom.xml} as the repository
     *                holds it
     * @return the {@code pom.xml}, or {@code null} when the project has none
     */
    public static @Nullable ProjectPom inRepository(RulesProject project, @Nullable Path folder) throws IOException {
        if (project.isLocalOnly()) {
            return folder == null ? null : inFolder(folder, folder);
        }
        var onDisk = folder == null ? null : folder.resolve(ProjectPom.FILE_NAME);
        var saved = onDisk != null && Files.isRegularFile(onDisk) ? Files.readString(onDisk) : null;
        var repository = project.getDesignRepository();
        if (!repository.supports().folders()) {
            return saved != null ? ProjectPom.of(saved) : inArchive(project);
        }
        var fileData = project.getFileData();
        var version = project.isOpenedOtherVersion() && fileData != null ? fileData.getVersion() : null;
        ProjectPom.Reader reader = path -> textOf(read(repository, path, version));
        var path = project.getRealPath() + "/" + ProjectPom.FILE_NAME;
        var text = saved != null ? saved : reader.read(path);
        var origin = repository.getId() + "@" + (version == null ? "head" : version);
        return text == null ? null : new ProjectPom(text, path, origin, reader);
    }

    /**
     * The {@code pom.xml} the archive of the project holds, as any file of it is read: the POMs it names are read from
     * the Maven repository.
     *
     * @return the {@code pom.xml}, or {@code null} when the project has none
     */
    public static @Nullable ProjectPom inArchive(AProject project) throws IOException {
        try {
            if (!project.hasArtefact(ProjectPom.FILE_NAME)
                    || !(project.getArtefact(ProjectPom.FILE_NAME) instanceof AProjectResource resource)) {
                return null;
            }
            try (var content = resource.getContent()) {
                return ProjectPom.of(new String(content.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (ProjectException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static @Nullable String textOf(@Nullable FileItem file) throws IOException {
        if (file == null) {
            return null;
        }
        try (file) {
            return new String(file.getStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Reads the file by its real path: the one in the repository a folder mapper maps. */
    private static @Nullable FileItem read(Repository repository, String name, @Nullable String version)
            throws IOException {
        var real = repository instanceof FolderMapper mapper ? mapper.getDelegate() : repository;
        return version == null ? real.read(name) : real.readHistory(name, version);
    }
}
