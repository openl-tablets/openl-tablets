package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

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
import org.openl.security.acl.repository.RepositoryAclServiceProvider;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

/**
 * Default {@link ProjectFileLookupService} implementation.
 *
 * <p>Walks up from the anchor folder to the repository root, collecting the same-named file found at
 * each level — the anchor itself first, then each ancestor, ordered nearest first. Descendants of the
 * anchor and sibling branches are not visited.
 *
 * <p>Files inside the anchor's project are resolved through the project's artefact tree, so the
 * working copy is reflected and zip-backed (flat) projects — whose inner files a repository listing
 * never exposes — are handled uniformly. Ancestor files above the project are read from the underlying
 * repository (after unwrapping {@link RepositoryDelegate} and {@link FolderMapper}); only
 * folder-supporting repositories expose a hierarchy outside a project, so flat repositories surface
 * the project's own files alone.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ProjectFileLookupServiceImpl implements ProjectFileLookupService {

    /**
     * Maximum size of a single file (in bytes) that the lookup is willing to surface.
     * Files larger than this are silently skipped so the response cannot grow unbounded.
     */
    static final long MAX_FILE_SIZE_BYTES = 1024L * 1024L;

    /**
     * Maximum number of files the response may contain. Once reached the scan stops collecting.
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
    private final RepositoryAclServiceProvider aclServiceProvider;

    @Override
    public List<FsNode> lookup(AProject project, Repository repository, String anchorPath, boolean includeContent)
            throws IOException {
        String fileName = FilePaths.name(FilePaths.trimSlashes(anchorPath));
        if (StringUtils.isBlank(fileName) || !isTextFile(fileName)) {
            return List.of();
        }
        String anchorDir = FilePaths.parent(FilePaths.trimSlashes(anchorPath));
        String base = FilePaths.trimSlashes(project.getRealPath());

        List<Candidate> candidates = new ArrayList<>();
        // Inside the project: the artefact tree reflects the working copy and unpacks flat projects.
        collectInProject(project, base, anchorDir, fileName, candidates);
        // Outside the project: only a folder design repository exposes anything beyond a project. The
        // current project is skipped here, since the artefact tree already covers it. A local-only
        // project has no design repository ({@code null}), so the search covers just its own files.
        // Each match is authorized individually, so the search never surfaces a file the user cannot read.
        if (repository != null && repository.supports().folders()) {
            collectFromRepository(unwrapRepository(repository), repository.getId(), anchorDir, fileName, base,
                    candidates);
        }
        return finish(candidates, anchorDir, includeContent);
    }

    @Override
    public List<FsNode> lookup(Repository repository, String anchorPath, boolean includeContent) throws IOException {
        String fileName = FilePaths.name(FilePaths.trimSlashes(anchorPath));
        if (StringUtils.isBlank(fileName) || !isTextFile(fileName)) {
            return List.of();
        }
        String anchorDir = FilePaths.parent(FilePaths.trimSlashes(anchorPath));
        List<Candidate> candidates = new ArrayList<>();
        collectFromRepository(unwrapRepository(repository), repository.getId(), anchorDir, fileName, null, candidates);
        return finish(candidates, anchorDir, includeContent);
    }

    private void collectInProject(AProjectFolder folder, String base, String anchorDir, String fileName,
                                  List<Candidate> out) {
        for (AProjectArtefact artefact : folder.getArtefacts()) {
            if (artefact.isFolder()) {
                collectInProject((AProjectFolder) artefact, base, anchorDir, fileName, out);
            } else {
                Candidate candidate = candidateFromArtefact(artefact, base, anchorDir, fileName);
                if (candidate != null) {
                    out.add(candidate);
                }
            }
        }
    }

    /**
     * Builds a candidate for a project file that matches by name, lies on the anchor's upward line and
     * is readable by the current user; {@code null} when any of these does not hold.
     */
    private Candidate candidateFromArtefact(AProjectArtefact artefact, String base, String anchorDir,
                                            String fileName) {
        if (!fileName.equals(artefact.getName()) || exceedsSizeLimit(artefact.getFileData())) {
            return null;
        }
        String rel = FilePaths.trimSlashes(artefact.getInternalPath());
        String path = base.isEmpty() ? rel : base + "/" + rel;
        if (!isAncestorOrSelf(FilePaths.parent(path), anchorDir)
                || !aclProjectsHelper.hasPermission(artefact, BasePermission.READ)) {
            return null;
        }
        var resource = (AProjectResource) artefact;
        return new Candidate(path, sizeOf(artefact.getFileData()), modifiedOf(artefact.getFileData()),
                () -> readContent(resource));
    }

    /**
     * Collects matches from a repository listing that lie on the anchor's upward line — the anchor
     * folder and its ancestors up to the repository root, never a descendant or a sibling branch —
     * keeping only files the current user is granted READ on. {@code excludeBase}, when set, drops the
     * current project (already covered by its artefact tree).
     */
    private void collectFromRepository(Repository repository, String repositoryId, String anchorDir, String fileName,
                                       String excludeBase, List<Candidate> out) throws IOException {
        var aclService = aclServiceProvider.getDesignRepoAclService();
        for (FileData data : repository.list("")) {
            String path = FilePaths.trimSlashes(data.getName());
            boolean match = !data.isDeleted()
                    && !exceedsSizeLimit(data)
                    && fileName.equals(FilePaths.name(path))
                    && !isExcluded(path, excludeBase)
                    && isAncestorOrSelf(FilePaths.parent(path), anchorDir)
                    && aclService.isGranted(repositoryId, path, true, BasePermission.READ);
            if (match) {
                out.add(new Candidate(path, sizeOf(data), modifiedOf(data), () -> readContent(repository, path)));
            }
        }
    }

    /**
     * Whether {@code path} is the excluded base project or one of its descendants — already covered by
     * the project's own artefact tree. Nothing is excluded when {@code excludeBase} is blank.
     */
    private static boolean isExcluded(String path, String excludeBase) {
        return !StringUtils.isBlank(excludeBase)
                && (path.equals(excludeBase) || path.startsWith(excludeBase + "/"));
    }

    /**
     * Whether a file in {@code dir} sits on the anchor's upward line: {@code dir} is the anchor folder
     * itself or one of its ancestors, up to the repository root. The walk goes up only — descendants of
     * the anchor and sibling branches are off the line and excluded.
     */
    private static boolean isAncestorOrSelf(String dir, String anchorDir) {
        return dir.isEmpty() || dir.equals(anchorDir) || anchorDir.startsWith(dir + "/");
    }

    /**
     * Orders candidates nearest to the anchor first, caps the count and (optionally) reads content.
     */
    private static List<FsNode> finish(List<Candidate> candidates, String anchorDir, boolean includeContent) {
        candidates.sort(Comparator
                .comparingInt((Candidate c) -> distance(anchorDir, FilePaths.parent(c.path())))
                .thenComparing(Candidate::path));
        List<FsNode> result = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (result.size() >= MAX_FILES_COUNT) {
                break;
            }
            String content = null;
            if (includeContent) {
                content = readContentQuietly(candidate);
                if (content == null) {
                    // Too large, missing, or unreadable — skip this one rather than fail the whole lookup.
                    continue;
                }
            }
            result.add(toNode(candidate, content));
        }
        return result;
    }

    /**
     * Reads a candidate's content, returning {@code null} when it is too large, missing or cannot be
     * read. A read failure on a single file is swallowed so one unreadable ancestor is skipped instead
     * of aborting the whole lookup.
     */
    private static String readContentQuietly(Candidate candidate) {
        try {
            return candidate.reader().read();
        } catch (IOException | RuntimeException e) {
            log.debug("Skipping unreadable file '{}'", candidate.path(), e);
            return null;
        }
    }

    private static FsNode toNode(Candidate candidate, String content) {
        String name = FilePaths.name(candidate.path());
        return FileNode.builder()
                .path(candidate.path())
                .name(name)
                .basePath(FilePaths.parent(candidate.path()))
                .extension(FileUtils.getExtension(name))
                .size(candidate.size())
                .lastModified(candidate.lastModified())
                .content(content)
                .build();
    }

    /**
     * Directory steps between two folders in the repository tree: up to their common ancestor, then
     * down to the target. A file in the anchor folder is distance {@code 0}, one in a child or parent
     * folder is distance {@code 1}, and so on. Drives the nearest-first ordering.
     */
    private static int distance(String fromDir, String toDir) {
        if (fromDir.equals(toDir)) {
            return 0;
        }
        String[] from = fromDir.isEmpty() ? new String[0] : fromDir.split("/");
        String[] to = toDir.isEmpty() ? new String[0] : toDir.split("/");
        int common = 0;
        while (common < from.length && common < to.length && from[common].equals(to[common])) {
            common++;
        }
        return (from.length - common) + (to.length - common);
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

    private static Long sizeOf(FileData data) {
        if (data == null || data.getSize() == FileData.UNDEFINED_SIZE) {
            return null;
        }
        return data.getSize();
    }

    private static ZonedDateTime modifiedOf(FileData data) {
        Date date = data == null ? null : data.getModifiedAt();
        return date == null ? null : date.toInstant().atZone(ZoneOffset.UTC);
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

    /**
     * A pending match: its repository-relative path, metadata and a way to read its content on demand,
     * so content is read only for the entries that survive ordering and the count cap.
     */
    private record Candidate(String path, Long size, ZonedDateTime lastModified, ContentReader reader) {
    }

    @FunctionalInterface
    private interface ContentReader {
        String read() throws IOException;
    }
}
