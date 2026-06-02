package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.files.FileNode;
import org.openl.studio.projects.model.files.FsNode;
import org.openl.studio.projects.validator.ProjectStateValidator;
import org.openl.util.FileUtils;
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
    public List<FsNode> searchAncestors(String lookupPath) {
        try {
            return fileLookupService.lookup(project, lookupPath, true, false).files().stream()
                    .map(match -> (FsNode) FileNode.builder()
                            .path(match.path())
                            .name(fileName(match.path()))
                            .basePath(parentPath(match.path()))
                            .extension(FileUtils.getExtension(fileName(match.path())))
                            .build())
                    .toList();
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

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String parentPath(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash);
    }
}
