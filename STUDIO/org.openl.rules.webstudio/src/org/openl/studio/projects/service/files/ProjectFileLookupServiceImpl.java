package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.RepositoryDelegate;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.projects.model.files.ProjectFileLookupResponse;
import org.openl.studio.projects.model.files.ProjectFileLookupResponse.ProjectFileMatch;
import org.openl.util.FileUtils;

/**
 * Default {@link ProjectFileLookupService} implementation.
 *
 * <p>Project-internal matches are resolved through the project's artefact tree so that
 * per-artefact permissions and zip-backed projects are handled uniformly.
 *
 * <p>Ancestor matches are read directly from the underlying raw repository (after
 * unwrapping {@link RepositoryDelegate} and {@link FolderMapper}), since those paths
 * live outside any project's artefact tree.
 */
@RequiredArgsConstructor
@Service
@Validated
public class ProjectFileLookupServiceImpl implements ProjectFileLookupService {

    /**
     * Maximum size of a single file (in bytes) that the lookup is willing to surface.
     * Files larger than this are silently skipped so the response cannot grow unbounded.
     */
    static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;

    /**
     * Maximum number of files the response may contain. Once reached the ancestor walk stops.
     */
    static final int MAX_FILES_COUNT = 32;

    /**
     * Whitelist of file extensions treated as UTF-8 text. Files outside the whitelist are skipped
     * — reading binary content as a UTF-8 string would corrupt it and bloat the response.
     */
    static final Set<String> TEXT_FILE_EXTENSIONS = Set.of(
            "md", "txt", "json", "xml", "yml", "yaml", "properties", "ini", "toml", "conf",
            "csv", "tsv", "log",
            "html", "htm", "css", "js", "ts", "jsx", "tsx",
            "java", "py", "sh", "sql"
    );

    private final AclProjectsHelper aclProjectsHelper;

    @Override
    public ProjectFileLookupResponse lookup(@NotNull AProject project,
                                            @NotBlank String path,
                                            boolean searchParents,
                                            boolean includeContent) throws IOException {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
        String relativePath = validatePath(path);

        List<ProjectFileMatch> matches = new ArrayList<>();
        addProjectMatch(matches, project, relativePath, includeContent);

        if (searchParents) {
            String leafName = fileName(relativePath);

            // Walk WITHIN the project: from the looked-up file's directory up to the project
            // root, looking for the leaf name at each ancestor. Goes through the project's
            // own artefact tree so it works for both folder and flat (zip-backed) repositories.
            String inProjectDir = parentOf(relativePath);
            while (!inProjectDir.isEmpty() && matches.size() < MAX_FILES_COUNT) {
                inProjectDir = parentOf(inProjectDir);
                String inProjectPath = inProjectDir.isEmpty() ? leafName : inProjectDir + "/" + leafName;
                addProjectMatch(matches, project, inProjectPath, includeContent);
            }

            // Walk OUTSIDE the project: from the project's parent directory up to the
            // repository root via the raw underlying repository. Only folder-supporting
            // repositories expose a directory hierarchy above a project, so flat repos
            // simply skip this phase.
            if (project.getRepository().supports().folders()) {
                Repository rawRepository = unwrapRepository(project.getRepository());
                String currentDir = trimSlashes(project.getRealPath());
                while (!currentDir.isEmpty() && matches.size() < MAX_FILES_COUNT) {
                    currentDir = parentOf(currentDir);
                    addAncestorMatch(matches, rawRepository, currentDir, leafName, includeContent);
                }
            }
        }

        return ProjectFileLookupResponse.builder().files(matches).build();
    }

    private void addProjectMatch(List<ProjectFileMatch> matches,
                                 AProject project,
                                 String relativePath,
                                 boolean includeContent) throws IOException {
        if (matches.size() >= MAX_FILES_COUNT) {
            return;
        }
        if (!isTextFile(fileName(relativePath))) {
            return;
        }
        AProjectArtefact found = findArtefactByPath(project, relativePath);
        if (found == null || found.isFolder()) {
            return;
        }
        if (exceedsSizeLimit(found.getFileData())) {
            return;
        }
        if (!aclProjectsHelper.hasPermission(found, BasePermission.READ)) {
            return;
        }
        var resource = (AProjectResource) found;
        String content = null;
        if (includeContent) {
            content = readContent(resource);
            if (content == null) {
                // Too large to surface (or unreadable) — skip rather than risk an unbounded read.
                return;
            }
        }
        matches.add(ProjectFileMatch.builder()
                .path(repositoryRelativePath(project, relativePath))
                .content(content)
                .build());
    }

    private void addAncestorMatch(List<ProjectFileMatch> matches,
                                  Repository repository,
                                  String dir,
                                  String relativePath,
                                  boolean includeContent) throws IOException {
        if (matches.size() >= MAX_FILES_COUNT) {
            return;
        }
        String fullPath = dir.isEmpty() ? relativePath : dir + "/" + relativePath;
        if (!isTextFile(fileName(fullPath))) {
            return;
        }
        FileData data = repository.check(fullPath);
        if (data == null || data.isDeleted()) {
            return;
        }
        if (exceedsSizeLimit(data)) {
            return;
        }
        String content = null;
        if (includeContent) {
            content = readContent(repository, fullPath);
            if (content == null) {
                // Too large to surface (or unreadable) — skip rather than risk an unbounded read.
                return;
            }
        }
        matches.add(ProjectFileMatch.builder()
                .path(fullPath)
                .content(content)
                .build());
    }

    private static boolean isTextFile(String fileName) {
        String ext = FileUtils.getExtension(fileName);
        return ext != null && !ext.isEmpty() && TEXT_FILE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    private static boolean exceedsSizeLimit(FileData data) {
        if (data == null) {
            return false;
        }
        long size = data.getSize();
        return size != FileData.UNDEFINED_SIZE && size > MAX_FILE_SIZE_BYTES;
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String readContent(AProjectResource resource) throws IOException {
        try (InputStream in = resource.getContent()) {
            return in == null ? null : readBoundedText(in);
        } catch (ProjectException e) {
            throw new ConflictException("file.read.failed.message");
        }
    }

    private static String readContent(Repository repository, String path) throws IOException {
        try (FileItem item = repository.read(path)) {
            if (item == null || item.getStream() == null) {
                return null;
            }
            return readBoundedText(item.getStream());
        }
    }

    /**
     * Reads UTF-8 text from the stream while keeping memory use bounded.
     *
     * <p>At most {@link #MAX_FILE_SIZE_BYTES} bytes are loaded. When the stream holds more,
     * the file is treated as too large and {@code null} is returned so the caller can skip it.
     *
     * <p>This protects against repositories that do not report a file size up front, where the
     * metadata size check is unable to reject an oversized file before it is read.
     */
    private static String readBoundedText(InputStream in) throws IOException {
        byte[] data = in.readNBytes((int) (MAX_FILE_SIZE_BYTES + 1));
        if (data.length > MAX_FILE_SIZE_BYTES) {
            return null;
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Looks up an artefact stored directly under the project root.
     *
     * <p>{@link AProjectFolder#createInternalArtefacts()} flattens the project tree by
     * storing every file with its full sub-path as the key (e.g. {@code "config/AGENTS.md"}),
     * so a direct lookup by the relative path is enough — no segment-by-segment walk needed.
     */
    private static AProjectArtefact findArtefactByPath(AProjectFolder rootFolder, String path) {
        if (!rootFolder.isFolder()) {
            return null;
        }
        try {
            return rootFolder.getArtefact(path);
        } catch (ProjectException e) {
            return null;
        }
    }

    private static String repositoryRelativePath(AProject project, String relativePath) {
        String real = trimSlashes(project.getRealPath());
        return real.isEmpty() ? relativePath : real + "/" + relativePath;
    }

    private static Repository unwrapRepository(Repository repo) {
        Repository current = repo;
        while (current instanceof RepositoryDelegate delegate) {
            current = delegate.getOriginal();
        }
        if (current instanceof FolderMapper mapper) {
            current = mapper.getDelegate();
        }
        return current;
    }

    private static String parentOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }

    private static String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String validatePath(String path) {
        if (path == null) {
            throw new BadRequestException("file.path.invalid.message");
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("file.path.invalid.message");
        }
        try {
            Repository.validatePath(trimmed);
        } catch (InvalidPathException e) {
            throw new BadRequestException("file.path.invalid.message");
        }
        return trimmed;
    }
}
