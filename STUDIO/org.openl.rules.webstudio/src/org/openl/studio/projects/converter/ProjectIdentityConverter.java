package org.openl.studio.projects.converter;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.studio.common.exception.ConflictException;
import org.openl.studio.common.exception.NotFoundException;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.service.ProjectIdentifierMapper;

/**
 * Resolves {@link AProject} from a project identity. The identity is either a project ID or a project name. Iterates a
 * chain of {@link ProjectResolveStrategy} strategies in order; the first strategy that returns at least one match is
 * authoritative. Multiple matches from the same strategy produce an ambiguity {@link ConflictException}.
 *
 * <p>A caller that knows which repository holds the project narrows the answer to it; the identity then resolves only
 * to a project of that repository.
 *
 * @author Vladyslav Pikus
 */
@Component
@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public class ProjectIdentityConverter implements Converter<String, RulesProject> {

    @Qualifier("designRepositoryAclService")
    private final RepositoryAclService designRepositoryAclService;
    private final List<ProjectResolveStrategy> strategies;
    private final ProjectIdentifierMapper projectIdentifierMapper;

    @Lookup
    public UserWorkspace getUserWorkspace() {
        return null;
    }

    @Override
    @Nonnull
    public RulesProject convert(String identity) {
        var project = resolveProjectIdentity(identity);
        if (project == null) {
            throw new NotFoundException("project.identifier.message");
        }
        if (!designRepositoryAclService.isGranted(project, List.of(BasePermission.READ))) {
            throw new SecurityException();
        }
        return project;
    }

    public RulesProject resolveProjectIdentity(String identity) {
        return resolveProjectIdentity(identity, null);
    }

    /**
     * The project the identity names, looked for in one repository when it is given.
     *
     * <p>Naming the repository settles a business name two repositories both carry. It cannot settle one that a
     * single repository carries in several folders — only an id tells those apart, and such a name stays ambiguous.
     *
     * <p>The strategy that answers first stays authoritative: an identity it answers for with a project of another
     * repository names no project here, rather than being handed on to the next strategy.
     */
    public RulesProject resolveProjectIdentity(String identity, @Nullable String repositoryId) {
        var workspace = getUserWorkspace();
        for (var strategy : strategies) {
            var matches = strategy.resolve(workspace, identity);
            if (!matches.isEmpty()) {
                var held = matches.stream().filter(project -> heldBy(project, repositoryId)).toList();
                return held.isEmpty() ? null : selectSingleMatch(identity, held);
            }
        }
        return null;
    }

    private static boolean heldBy(RulesProject project, @Nullable String repositoryId) {
        if (repositoryId == null) {
            return true;
        }
        var designRepository = project.getDesignRepository();
        return designRepository != null && repositoryId.equals(designRepository.getId());
    }

    private RulesProject selectSingleMatch(String identity, List<RulesProject> matches) {
        if (matches.size() > 1) {
            var candidates = matches.stream()
                    .map(projectIdentifierMapper::map)
                    .map(ProjectIdModel::encode)
                    .collect(Collectors.joining(", "));
            throw new ConflictException("project.identifier.ambiguous.message", identity, candidates);
        }
        return matches.getFirst();
    }

}
