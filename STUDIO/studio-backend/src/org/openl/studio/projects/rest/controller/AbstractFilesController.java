package org.openl.studio.projects.rest.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import org.openl.rules.common.ProjectException;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.FileWriteOutcome;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.files.ProjectFilesService.UploadedFile;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;

/**
 * Shared logic for the files API exposed over the {@code /{mount}/files/{*path}} address space.
 *
 * <p>Subclasses keep the framework-bound endpoint methods so each mount resolves its own
 * {@link FileRoot} (a project working copy or a repository subtree); the request handling lives here
 * once. A mount with a working copy refreshes its derived state through {@link #postWrite()}, and follows up an
 * upload through {@link #postUpload(FileRoot, Collection)}.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public abstract class AbstractFilesController {

    protected final ProjectFilesService filesService;
    protected final BeanValidationProvider validationProvider;
    protected final FileCriteriaQueryValidator queryValidator;

    /**
     * Refreshes state derived from the mount after a write. The default does nothing; a mount backed
     * by a working copy overrides it.
     */
    protected void postWrite() {
        // no-op by default
    }

    /**
     * Follows up the files an upload has written, once the mount is refreshed. The default does nothing; a mount
     * backed by a working copy overrides it.
     *
     * @param root     the mount written to
     * @param uploaded the mount-relative paths of the files written
     */
    protected void postUpload(FileRoot root, Collection<String> uploaded) {
        // no-op by default
    }

    /**
     * Handles a multipart create: a folder path uploads its files as one operation, a file path
     * writes a single resource as {@link #handleCreateRaw} does.
     *
     * @return {@code 201 Created} for a folder upload and a created file, {@code 204 No Content} for a file that
     *         was already there
     */
    protected ResponseEntity<Void> handleCreate(FileRoot root, String path, List<MultipartFile> files,
                                                boolean createFolders, ConflictPolicy conflictPolicy)
            throws IOException {
        if (isFolderPath(path)) {
            List<String> uploaded;
            try {
                uploaded = filesService.uploadFiles(root, stripSlashes(path), toUploadedFiles(files), conflictPolicy);
            } finally {
                postWrite();
            }
            postUpload(root, uploaded);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        return handleCreateRaw(root, path, createFolders, conflictPolicy, files.getFirst().getInputStream());
    }

    /**
     * Handles a create of a single resource, following the conflict policy when a file is already there.
     *
     * @return {@code 201 Created} for a created file, {@code 204 No Content} for a file that was already there:
     *         replaced, or left as it is for {@link ConflictPolicy#SKIP}
     */
    protected ResponseEntity<Void> handleCreateRaw(FileRoot root, String path, boolean createFolders,
                                                   ConflictPolicy conflictPolicy, InputStream content) {
        var filePath = stripLeadingSlash(path);
        FileWriteOutcome outcome;
        try {
            outcome = filesService.writeResource(root, filePath, content, createFolders, conflictPolicy);
        } finally {
            postWrite();
        }
        // A file left as it is was not uploaded, so nothing of it is followed up.
        postUpload(root, outcome == FileWriteOutcome.KEPT ? List.of() : List.of(filePath));
        return ResponseEntity.status(outcome == FileWriteOutcome.CREATED ? HttpStatus.CREATED : HttpStatus.NO_CONTENT)
                .build();
    }

    /**
     * Expands an uploaded ZIP archive into a folder.
     */
    protected void handleUploadArchive(FileRoot root, String path, boolean createFolders,
                                       ConflictPolicy conflictPolicy, InputStream content) throws IOException {
        if (!isFolderPath(path)) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        List<String> uploaded;
        try {
            uploaded = filesService.uploadArchive(root, stripSlashes(path), content, createFolders, conflictPolicy);
        } finally {
            postWrite();
        }
        postUpload(root, uploaded);
    }

    /**
     * Serves a file download, a folder zip download, or a folder/file listing depending on the path
     * and parameters.
     */
    // The endpoint answers with a listing, a file node or the file content, depending on the path and the view.
    // The handler takes every request parameter of the file endpoints that delegate to it.
    @SuppressWarnings({"java:S107", "java:S1452"})
    protected ResponseEntity<?> handleGetFile(FileRoot root, String path, String view, String download,
                                              Set<String> extensions, String namePattern, boolean foldersOnly,
                                              boolean recursive, FileViewMode viewMode, String version,
                                              HttpServletResponse response, @Nullable String rootArchiveName)
            throws ProjectException, IOException {
        if (isFolderPath(path)) {
            var basePath = stripSlashes(path);
            if (download != null) {
                String zipName = getArchiveName(basePath, rootArchiveName);
                response.setContentType("application/zip");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(zipName));
                filesService.writeFolderAsZip(root, basePath, response.getOutputStream(), version);
                return null;
            }
            var queryBuilder = FileCriteriaQuery.builder()
                    .basePath(basePath.isEmpty() ? null : basePath)
                    .namePattern(namePattern)
                    .foldersOnly(foldersOnly);
            if (extensions != null) {
                queryBuilder.extensions(extensions);
            }
            var query = queryBuilder.build();
            validationProvider.validate(query, queryValidator);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(filesService.getResources(root, query, recursive, viewMode, version));
        }
        var filePath = stripLeadingSlash(path);
        if ("meta".equalsIgnoreCase(view)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(filesService.getNode(root, filePath, version));
        }
        // Stream the content to the response in the request thread, without buffering the whole file in memory.
        // Existence and permissions are resolved first, so a missing or forbidden file fails before anything is
        // written.
        var resource = filesService.getResource(root, filePath, version);
        var fileName = resource.getName();
        response.setContentType(MediaTypeFactory.getMediaType(fileName)
                .orElse(MediaType.APPLICATION_OCTET_STREAM).toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(fileName));
        try (var content = resource.getContent()) {
            content.transferTo(response.getOutputStream());
        }
        return null;
    }

    private static String getArchiveName(String basePath, @Nullable String rootArchiveName) {
        if (basePath.isEmpty()) {
            return rootArchiveName == null || rootArchiveName.isBlank() ? "files.zip" : rootArchiveName;
        }
        return basePath.substring(basePath.lastIndexOf('/') + 1) + ".zip";
    }

    /**
     * Updates a resource from a multipart file.
     */
    protected void handleUpdate(FileRoot root, String path, MultipartFile file) throws IOException {
        try {
            filesService.updateResource(root, stripLeadingSlash(path), file.getInputStream());
        } finally {
            postWrite();
        }
        postUpload(root, List.of(stripLeadingSlash(path)));
    }

    /**
     * Updates a resource from a raw octet stream.
     */
    protected void handleUpdateRaw(FileRoot root, String path, InputStream content) {
        try {
            filesService.updateResource(root, stripLeadingSlash(path), content);
        } finally {
            postWrite();
        }
        postUpload(root, List.of(stripLeadingSlash(path)));
    }

    /**
     * Creates a folder at the path.
     */
    protected void handleCreateFolder(FileRoot root, String path, boolean createFolders) {
        if (!isFolderPath(path)) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        try {
            filesService.createFolder(root, stripSlashes(path), createFolders);
        } finally {
            postWrite();
        }
    }

    /**
     * Deletes a resource (file or folder) at the path.
     */
    protected void handleDelete(FileRoot root, String path) {
        try {
            filesService.deleteResource(root, stripLeadingSlash(path));
        } finally {
            postWrite();
        }
    }

    // Reading an uploaded file throws an IOException, which a stream lambda cannot pass on.
    @SuppressWarnings("java:S9391")
    private static List<UploadedFile> toUploadedFiles(List<MultipartFile> files) throws IOException {
        var uploaded = new ArrayList<UploadedFile>();
        for (MultipartFile file : files) {
            uploaded.add(new UploadedFile(file.getOriginalFilename(), file.getBytes()));
        }
        return uploaded;
    }

    /**
     * Determines whether the captured path addresses a folder. A trailing slash, or the
     * empty/root path, denotes a folder; any other path denotes a file.
     */
    private static boolean isFolderPath(String path) {
        return path == null || path.isEmpty() || path.endsWith("/");
    }

    /**
     * Strips leading and trailing slashes from the path captured by {@code {*path}}.
     */
    private static String stripSlashes(String path) {
        var result = stripLeadingSlash(path);
        while (result != null && result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Strips the leading slash from the path captured by {@code {*path}}.
     * Spring's catch-all path variable includes a leading '/' (e.g., "/folder/file.xlsx").
     */
    private static String stripLeadingSlash(String path) {
        if (path != null && path.startsWith("/")) {
            return path.substring(1);
        }
        return path;
    }
}
