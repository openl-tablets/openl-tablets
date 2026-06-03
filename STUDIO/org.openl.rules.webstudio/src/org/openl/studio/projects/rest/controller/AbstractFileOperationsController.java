package org.openl.studio.projects.rest.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.openl.studio.projects.model.files.FilePathPairRequest;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.service.files.FileRoot;
import org.openl.studio.projects.service.files.FileSearchQuery;
import org.openl.studio.projects.service.files.ProjectFilesService;

/**
 * Shared logic for two-path file operations (copy and move) and search, which live outside the
 * {@code /{mount}/files/{*path}} address space so a command name can never shadow a real file.
 *
 * <p>Subclasses keep the framework-bound endpoint methods so each mount resolves its own
 * {@link FileRoot}; the request handling lives here once. A mount with a working copy refreshes its
 * derived state through {@link #postWrite()}.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public abstract class AbstractFileOperationsController {

    protected final ProjectFilesService filesService;

    /**
     * Refreshes state derived from the mount after a write. The default does nothing; a mount backed
     * by a working copy overrides it.
     */
    protected void postWrite() {
        // no-op by default
    }

    protected void handleCopy(FileRoot root, FilePathPairRequest request) {
        try {
            filesService.copyResource(root, request.sourcePath(), request.destinationPath());
        } finally {
            postWrite();
        }
    }

    protected void handleMove(FileRoot root, FilePathPairRequest request) {
        try {
            filesService.moveResource(root, request.sourcePath(), request.destinationPath());
        } finally {
            postWrite();
        }
    }

    protected List<FsNode> handleSearch(FileRoot root, FileSearchQuery query) {
        return filesService.search(root, query);
    }
}
