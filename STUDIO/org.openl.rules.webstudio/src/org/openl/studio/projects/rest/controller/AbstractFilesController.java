package org.openl.studio.projects.rest.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.openl.rules.common.ProjectException;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.utils.WebTool;
import org.openl.studio.common.validation.BeanValidationProvider;
import org.openl.studio.projects.service.files.ConflictPolicy;
import org.openl.studio.projects.service.files.FileCriteriaQuery;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileViewMode;
import org.openl.studio.projects.service.files.ProjectFilesService;
import org.openl.studio.projects.service.files.ProjectFilesService.UploadedFile;
import org.openl.studio.projects.validator.file.FileCriteriaQueryValidator;

/**
 * Shared logic for the files API exposed over the {@code /{mount}/files/{*path}} address space.
 *
 * <p>Subclasses keep the framework-bound endpoint methods so each mount resolves its own
 * {@link FileRoot} (a project working copy or a repository subtree); the request handling lives here
 * once. A mount with a working copy refreshes its derived state through {@link #postWrite()}.
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
     * Handles a multipart create: a folder path uploads its files as one operation, a file path
     * creates a single resource.
     */
    protected void handleCreate(FileRoot root, String path, List<MultipartFile> files,
                                boolean createFolders, ConflictPolicy conflictPolicy) throws IOException {
        try {
            if (isFolderPath(path)) {
                filesService.uploadFiles(root, stripSlashes(path), toUploadedFiles(files), conflictPolicy);
            } else {
                filesService.createResource(root, stripLeadingSlash(path), files.get(0).getInputStream(), createFolders);
            }
        } finally {
            postWrite();
        }
    }

    /**
     * Handles a raw octet-stream create of a single resource.
     */
    protected void handleCreateRaw(FileRoot root, String path, boolean createFolders, InputStream content) {
        try {
            filesService.createResource(root, stripLeadingSlash(path), content, createFolders);
        } finally {
            postWrite();
        }
    }

    /**
     * Expands an uploaded ZIP archive into a folder.
     */
    protected void handleUploadArchive(FileRoot root, String path, boolean createFolders,
                                       ConflictPolicy conflictPolicy, InputStream content) throws IOException {
        if (!isFolderPath(path)) {
            throw new BadRequestException("file.path.requires.content.message");
        }
        try {
            filesService.uploadArchive(root, stripSlashes(path), content, createFolders, conflictPolicy);
        } finally {
            postWrite();
        }
    }

    /**
     * Serves a file download, a folder zip download, or a folder/file listing depending on the path
     * and parameters.
     */
    protected ResponseEntity<?> handleGetFile(FileRoot root, String path, String view, String download,
                                              Set<String> extensions, String namePattern, boolean foldersOnly,
                                              boolean recursive, FileViewMode viewMode, String version)
            throws ProjectException, IOException {
        if (isFolderPath(path)) {
            var basePath = stripSlashes(path);
            if (download != null) {
                String zipName = (basePath.isEmpty()
                        ? "files" : basePath.substring(basePath.lastIndexOf('/') + 1)) + ".zip";
                StreamingResponseBody body = out -> filesService.writeFolderAsZip(root, basePath, out, version);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/zip"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(zipName))
                        .body(body);
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
        var resource = filesService.getResource(root, filePath, version);
        var output = new ByteArrayOutputStream();
        try (var stream = resource.getContent()) {
            stream.transferTo(output);
        }
        String fileName = resource.getName();
        return ResponseEntity.ok()
                .contentType(MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, WebTool.getContentDispositionValue(fileName))
                .body(output.toByteArray());
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

    private static List<UploadedFile> toUploadedFiles(List<MultipartFile> files) throws IOException {
        List<UploadedFile> uploaded = new ArrayList<>();
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
