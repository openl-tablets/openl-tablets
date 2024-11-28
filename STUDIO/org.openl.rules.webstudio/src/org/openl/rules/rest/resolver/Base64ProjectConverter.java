package org.openl.rules.rest.resolver;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.exception.NotFoundException;
import org.openl.rules.workspace.dtr.FolderMapper;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * Resolves {@link AProject} from base64 projectId. It's used to remove duplicated code in Spring Controllers and make
 * it more clear.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
public class Base64ProjectConverter implements Converter<String, RulesProject> {

    static final String SEPARATOR = ":";

    private final RepositoryAclService designRepositoryAclService;

    public Base64ProjectConverter(
            @Qualifier("designRepositoryAclService") RepositoryAclService designRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    public RulesProject convert(String projectId) {
        var project = resolveProjectIdentity(projectId);
        if (project == null) {
            throw new NotFoundException("project.identifier.message");
        }
        if (!designRepositoryAclService.isGranted(project, List.of(AclPermission.VIEW))) {
            throw new SecurityException();
        }
        return project;
    }

    public RulesProject resolveProjectIdentity(String projectId) {
        try {
            return decodeProjectIdentifier(projectId);
        } catch (ProjectException e) {
            var ex = new NotFoundException("project.identifier.message");
            ex.initCause(e);
            throw ex;
        }
    }

    private RulesProject decodeProjectIdentifier(String id) throws ProjectException {
        var decoded = new String(Base64.getDecoder().decode(id.getBytes(UTF_8)), UTF_8);
        var parts = decoded.indexOf(SEPARATOR);
        if (parts == -1) {
            throw new IllegalArgumentException("Invalid projectId: " + id);
        }
        var repoId = decoded.substring(0, parts);
        var projectName = decoded.substring(parts + 1);
        var workspace = getUserWorkspace();
        try {
            return workspace.getProject(repoId, projectName);
        } catch (ProjectException e) {
            var repository = workspace.getDesignTimeRepository().getRepository(repoId);
            if (repository != null && repository.supports().mappedFolders()) {
                var mappedRepository = (FolderMapper) repository;
                var businessName = mappedRepository.getBusinessName(projectName);
                if (!Objects.equals(businessName, projectName)) {
                    try {
                        return workspace.getProject(repoId, businessName);
                    } catch (ProjectException e1) {
                        e.addSuppressed(e1);
                    }
                }
            }
            throw e;
        }
    }

}
