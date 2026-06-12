package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.StringUtils;

/**
 * {@link FileRoot} backed by a project's working copy.
 *
 * <p>Reads of the current state use the workspace copy; historical reads and the ancestor walk use
 * the design repository. Authorization and modifiability follow the workspace project's rules.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
public class ProjectFileRoot implements FileRoot {

    private final RulesProject project;
    private final AclProjectsHelper aclProjectsHelper;
    private final ProjectStateValidator projectStateValidator;
    private final ProjectFileLookupService fileLookupService;

    @Override
    public AProjectFolder readFolder(String version) {
        if (StringUtils.isBlank(version)) {
            return wrap(project);
        }
        var historical = new AProject(project.getDesignRepository(), project.getDesignFolderName(), version);
        try {
            if (historical.getFileData() == null) {
                throw new NotFoundException("file.version.not.found.message");
            }
            return wrap(historical);
        } catch (NotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new NotFoundException("file.version.not.found.message");
        }
    }

    @Override
    public AProjectFolder writeFolder() {
        return project;
    }

    @Override
    public void requireReadable() {
        if (!aclProjectsHelper.hasPermission(project, BasePermission.READ)) {
            throw new ForbiddenException("default.message");
        }
    }

    @Override
    public void requireModifiable() {
        if (!projectStateValidator.canModify(project)) {
            throw new ConflictException("project.status.update.failed.message");
        }
        if (!aclProjectsHelper.hasPermission(project, BasePermission.WRITE)) {
            throw new ForbiddenException("default.message");
        }
    }

    @Override
    public boolean supportsAtomicWrite() {
        // The working copy stages writes and commits them on check-in, so they are written per file.
        return false;
    }

    @Override
    public void writeBatch(List<FileItem> items, String comment) {
        throw new UnsupportedOperationException("Workspace mount is written per file");
    }

    @Override
    public List<FsNode> searchAncestors(String lookupPath) {
        // The project's artefact tree covers files inside it (working copy, flat projects included).
        // Outside the project the search spans the whole design repository, not just this project, so
        // resolve the design repository explicitly: getRepository() is the local working copy for an
        // open-for-editing project. A local-only project has no design repository (null), so the search
        // stays within the project. getRealPath() is the project's matching repository-internal path,
        // so the anchor lands in the same namespace the search walks.
        String real = FilePaths.trimSlashes(project.getRealPath());
        String anchor = real.isEmpty() ? lookupPath : real + "/" + lookupPath;
        try {
            return fileLookupService.lookup(project, project.getDesignRepository(), anchor, true);
        } catch (IOException e) {
            throw new ConflictException("file.read.failed.message");
        }
    }

    /**
     * Wraps a project (current or historical) in a detached folder over the same repository and path.
     */
    private static AProjectFolder wrap(AProject source) {
        AProjectFolder folder = new AProjectFolder(new HashMap<>(),
                source.getProject(), source.getRepository(), source.getFolderPath());
        source.getArtefacts().forEach(folder::addArtefact);
        return folder;
    }
}
