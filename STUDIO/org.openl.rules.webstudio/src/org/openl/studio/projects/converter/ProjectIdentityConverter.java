package org.openl.studio.projects.converter;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;

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
        var workspace = getUserWorkspace();
        for (var strategy : strategies) {
            var matches = strategy.resolve(workspace, identity);
            if (!matches.isEmpty()) {
                return selectSingleMatch(identity, matches);
            }
        }
        return null;
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
