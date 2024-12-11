package org.openl.rules.rest.service;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.model.ProjectLockInfo;
import org.openl.rules.rest.model.ProjectViewModel;
import org.openl.rules.webstudio.service.OpenLProjectService;
import org.openl.security.acl.permission.AclPermission;
import org.openl.security.acl.repository.RepositoryAclService;
import org.openl.util.CollectionUtils;

/**
 * Abstract project service.
 *
 * @author Vladyslav Pikus
 */
@ParametersAreNonnullByDefault
public abstract class AbstractProjectService<T extends AProject> implements ProjectService<T> {

    private static final Predicate<AProject> ALL_PROJECTS = project -> true;

    protected static final String PROJECT_ID_SEPARATOR = ":";

    protected final RepositoryAclService designRepositoryAclService;
    private final OpenLProjectService projectService;

    public AbstractProjectService(RepositoryAclService designRepositoryAclService, OpenLProjectService projectService) {
        this.designRepositoryAclService = designRepositoryAclService;
        this.projectService = projectService;
    }

    @Override
    @Nonnull
    public List<ProjectViewModel> getProjects(ProjectCriteriaQuery query) {
        var criteriaFilter = buildFilterCriteria(query)
                .and(proj -> designRepositoryAclService.isGranted(proj, List.of(AclPermission.VIEW)))
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
            filter = project -> projectService
                    .isProjectHasTags(project.getRepository().getId(), project.getRealPath(), query.getTags());
        }
        return filter;
    }

    protected abstract Stream<T> getProjects0(ProjectCriteriaQuery query);

    protected ProjectViewModel.Builder mapProjectResponse(T src) {
        var repository = src.getRepository();
        var builder = ProjectViewModel.builder()
                .name(src.getBusinessName())
                .id(buildProjectId(repository.getId(), resolveProjectName(src)))
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

        var tags = projectService.getTagsForProject(repository.getId(), src.getRealPath());
        if (CollectionUtils.isNotEmpty(tags)) {
            tags.forEach(tag -> builder.addTag(tag.getType().getName(), tag.getName()));
        }

        return builder;
    }

    private String buildProjectId(String repositoryId, String projectName) {
        var rawProjectId = repositoryId + PROJECT_ID_SEPARATOR + projectName;
        return Base64.getEncoder().encodeToString(rawProjectId.getBytes(StandardCharsets.UTF_8));
    }

    protected String resolveProjectName(T src) {
        return src.getName();
    }

}
