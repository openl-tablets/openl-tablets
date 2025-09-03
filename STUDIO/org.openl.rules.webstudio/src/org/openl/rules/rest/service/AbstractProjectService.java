package org.openl.rules.rest.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import jakarta.annotation.Nonnull;

import org.springframework.security.acls.domain.BasePermission;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.model.ProjectIdModel;
import org.openl.rules.rest.model.ProjectLockInfo;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.security.acl.repository.RepositoryAclService;

/**
 * Abstract project service.
 *
 * @author Vladyslav Pikus
 */
@ParametersAreNonnullByDefault
public abstract class AbstractProjectService<T extends AProject> implements ProjectService<T> {

    private static final Predicate<AProject> ALL_PROJECTS = project -> true;

    protected final RepositoryAclService designRepositoryAclService;

    public AbstractProjectService(RepositoryAclService designRepositoryAclService) {
        this.designRepositoryAclService = designRepositoryAclService;
    }

    @Override
    @Nonnull
    public List<ProjectViewModel> getProjects(ProjectCriteriaQuery query) {
        var criteriaFilter = buildFilterCriteria(query)
                .and(proj -> designRepositoryAclService.isGranted(proj, List.of(BasePermission.READ)))
                .and(buildTagsFilterCriteria(query));
        return getProjects0(query).filter(criteriaFilter)
                .sorted(Comparator.comparing(AProject::getBusinessName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapProjectResponse)
                .map(ProjectViewModel.Builder::build)
                .collect(Collectors.toList());
    }

    @Nonnull
    protected Predicate<AProject> buildFilterCriteria(ProjectCriteriaQuery query) {
        return ALL_PROJECTS;
    }

    @Nonnull
    private Predicate<AProject> buildTagsFilterCriteria(ProjectCriteriaQuery query) {
        Predicate<AProject> filter = ALL_PROJECTS;
        if (!query.getTags().isEmpty()) {
            filter = project ->
                    project instanceof RulesProject
                            && projectHasTags((RulesProject) project, query.getTags());
        }
        return filter;
    }

    public boolean projectHasTags(RulesProject project, Map<String, String> tags) {
        return project.getLocalTags().entrySet().stream().anyMatch(tagValues ->
                tags.containsKey(tagValues.getKey()) && Objects.equals(tags.get(tagValues.getKey()), tagValues.getValue())
        );
    }

    protected abstract Stream<T> getProjects0(ProjectCriteriaQuery query);

    protected ProjectViewModel.Builder mapProjectResponse(T src) {
        var repository = src.getRepository();
        var builder = ProjectViewModel.builder()
                .name(src.getBusinessName())
                .id(ProjectIdModel.builder()
                        .repository(repository.getId())
                        .projectName(resolveProjectName(src))
                        .build())
                .repository(repository.getId());
        var fileData = src.getFileData();
        if (fileData != null) {
            Optional.ofNullable(fileData.getAuthor()).map(UserInfo::getName).ifPresent(builder::modifiedBy);
            Optional.ofNullable(fileData.getModifiedAt())
                    .map(Date::toInstant)
                    .map(instant -> ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()))
                    .ifPresent(builder::modifiedAt);
            Optional.ofNullable(fileData.getVersion()).ifPresent(builder::revision);
            Optional.ofNullable(fileData.getComment()).ifPresent(builder::comment);
        }
        if (!src.isOpenedForEditing() && src.isLocked()) {
            var lockInfo = src.getLockInfo();
            builder.lockInfo(ProjectLockInfo.builder()
                    .lockedBy(lockInfo.getLockedBy())
                    .lockedAt(ZonedDateTime.ofInstant(lockInfo.getLockedAt(), ZoneId.systemDefault()))
                    .build());
        }
        var designRepository = repository;
        if (src instanceof UserWorkspaceProject) {
            var workspaceProject = (UserWorkspaceProject) src;
            designRepository = workspaceProject.getDesignRepository();
            builder.status(workspaceProject.getStatus()).branch(workspaceProject.getBranch());
        } else {
            var features = repository.supports();
            if (features.branches()) {
                Optional.ofNullable(fileData).map(FileData::getBranch).ifPresent(builder::branch);
            }
        }

        if (designRepository != null && designRepository.supports().mappedFolders()) {
            var path = src.getRealPath().replace('\\', '/');
            builder.path(path);
        }

        if (src instanceof RulesProject) {
            RulesProject rulesProject = (RulesProject) src;
            var tags = rulesProject.getLocalTags();
            if (tags != null) {
                tags.forEach(builder::addTag);
            }
        }

        return builder;
    }

    protected String resolveProjectName(T src) {
        return src.getName();
    }

}
