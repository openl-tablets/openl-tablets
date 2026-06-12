package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.repository.api.ChangesetType;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.util.StringUtils;

/**
 * {@link FileRoot} backed by a design repository, rooted at the repository root.
 *
 * <p>The mount addresses files across the repository on an already-resolved branch. There is no
 * workspace copy, so reads and writes go straight to the repository. Authorization is checked at
 * the repository path, consistent with how project artefacts are authorized.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class RepoFileRoot implements FileRoot {

    /**
     * The mount is rooted at the repository root, so every path is repository-relative.
     */
    private static final String ROOT_PATH = "";

    private final Repository repository;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectFileLookupService fileLookupService;

    @Override
    public AProjectFolder readFolder(String version) {
        try {
            return buildTree(StringUtils.isBlank(version) ? null : version);
        } catch (NotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            if (StringUtils.isNotBlank(version)) {
                throw new NotFoundException("file.version.not.found.message");
            }
            throw e;
        }
    }

    @Override
    public AProjectFolder writeFolder() {
        return buildTree(null);
    }

    @Override
    public void requireReadable() {
        if (!aclProjectsHelper.hasPermission(new AProject(repository, ROOT_PATH), BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
    }

    @Override
    public void requireModifiable() {
        if (!aclProjectsHelper.hasPermission(new AProject(repository, ROOT_PATH), BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
    }

    @Override
    public List<FsNode> searchAncestors(String lookupPath) {
        // The mount already addresses the repository by real, repository-relative paths, so the
        // lookup path is the anchor as-is. The search walks up from the anchor to the repository root.
        try {
            return fileLookupService.lookup(repository, lookupPath, true);
        } catch (IOException e) {
            throw new ConflictException("file.read.failed.message");
        }
    }

    @Override
    public boolean supportsAtomicWrite() {
        return true;
    }

    @Override
    public void writeBatch(List<FileItem> items, String comment) {
        if (items.isEmpty()) {
            return;
        }
        var folderData = new FileData();
        folderData.setName(ROOT_PATH);
        folderData.setComment(comment);
        try {
            // DIFF adds and overwrites only the listed files in one commit, leaving others intact.
            // The author is stamped by the AuthoringRepository wrapper.
            repository.save(folderData, items, ChangesetType.DIFF);
        } catch (IOException e) {
            throw new ConflictException("file.archive.upload.failed.message");
        }
    }

    /**
     * Builds a navigable folder tree rooted at the repository subtree.
     *
     * <p>A repository lists its files as a flat set of full paths. They are regrouped by their first
     * path segment into child folders at non-empty paths, so each child builds its own sub-hierarchy
     * and supports navigation, listing and writes.
     */
    private AProjectFolder buildTree(String version) {
        var source = new AProject(repository, ROOT_PATH, version);
        var root = new AProjectFolder(new HashMap<>(), source.getProject(), repository, ROOT_PATH);
        Map<String, AProjectFolder> topFolders = new HashMap<>();
        for (AProjectArtefact artefact : listArtefacts(source, version)) {
            String path = artefact.getFileData().getName();
            int slash = path.indexOf('/');
            if (slash < 0) {
                root.addArtefact(artefact);
            } else {
                String top = path.substring(0, slash);
                topFolders.computeIfAbsent(top,
                        name -> new AProjectFolder(new HashMap<>(), source.getProject(), repository, name))
                        .addArtefact(artefact);
            }
        }
        topFolders.values().forEach(root::addArtefact);
        return root;
    }

    /**
     * Lists the artefacts assembled into the tree.
     *
     * <p>The latest revision is read through the project artefact API. A historical revision is listed
     * directly from the repository and wrapped as historic resources, so each read resolves the
     * requested version. This is needed because the mount root carries no metadata of its own to drive
     * a versioned listing through the project artefact API.
     */
    private Iterable<? extends AProjectArtefact> listArtefacts(AProject source, String version) {
        if (version == null) {
            return source.getArtefacts();
        }
        try {
            return repository.listFiles(ROOT_PATH, version).stream()
                    .filter(fileData -> !fileData.isDeleted())
                    .map(fileData -> new AProjectResource(source.getProject(), repository, fileData))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
