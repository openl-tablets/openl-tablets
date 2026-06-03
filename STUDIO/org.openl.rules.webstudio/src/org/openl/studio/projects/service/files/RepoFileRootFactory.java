package org.openl.studio.projects.service.files;

import java.io.IOException;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.util.StringUtils;

/**
 * Builds a {@link FileRoot} over a design repository, optionally scoped to a branch.
 *
 * @author Yury Molchan
 */
@Component
@RequiredArgsConstructor
public class RepoFileRootFactory {

    private final AclProjectsHelper aclProjectsHelper;
    private final UserManagementService userManagementService;
    private final ProjectFileLookupService fileLookupService;

    public FileRoot of(Repository repository, String branch) {
        // The design repository is wrapped in a (secured) MappedRepository that presents a virtual
        // "<base>/<business-name>:<hash>/..." namespace. Unwrap to the underlying repository so the
        // mount addresses files by their real, business-named, repository-relative paths. Per-artefact
        // ACL is enforced by the service against this same internal path, so authorization is unaffected.
        Repository resolved = repository instanceof FolderMapper mapper ? mapper.getDelegate() : repository;
        if (StringUtils.isNotBlank(branch)) {
            if (!resolved.supports().branches()) {
                throw new ConflictException("project.branch.unsupported.message");
            }
            try {
                resolved = ((BranchRepository) resolved).forBranch(branch);
            } catch (IOException e) {
                throw new NotFoundException("file.branch.not.found.message");
            }
        }
        Repository authored = resolved instanceof BranchRepository branchRepo
                ? new AuthoringRepository(branchRepo, currentAuthor())
                : resolved;
        return new RepoFileRoot(authored, "", aclProjectsHelper, fileLookupService);
    }

    /**
     * The current user, used as the git commit author for repository-mount writes.
     */
    private UserInfo currentAuthor() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Optional.ofNullable(userManagementService.getUser(username))
                .map(user -> new UserInfo(user.getUsername(), user.getEmail(), user.getDisplayName()))
                .orElseGet(() -> new UserInfo(username));
    }
}
