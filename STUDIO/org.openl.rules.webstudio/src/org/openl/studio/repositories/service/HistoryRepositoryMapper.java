package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.common.utils.DateTimes;
import org.openl.studio.repositories.model.ProjectRevision;

/**
 * {@link Repository} history mapper
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class HistoryRepositoryMapper {

    /** How many characters of a revision identify it on screen. */
    private static final int SHORT_REVISION_LENGTH = 6;

    /** A parsed commit message either splits into exactly this many parts or is not a templated one. */
    private static final int COMMENT_PARTS = 3;

    private final Repository repository;
    private final Comments commentService;

    /**
     * Gets project history log filtered by {@code globalFilter} if present
     *
     * @param name         target project full name
     * @param globalFilter global filer allows regexp.
     * @param pageable     page to display
     * @return paged history result
     * @throws IOException error
     */
    public PageResponse<ProjectRevision> getProjectHistory(String name,
                                                           String globalFilter,
                                                           boolean techRevs,
                                                           Pageable pageable) throws IOException {
        List<FileData> history;
        if (repository.supports().searchable()) {
            history = ((SearchableRepository) repository).listHistory(name, globalFilter, techRevs, pageable);
            // For searchable repos, we don't have total count
        } else {
            pageable = Page.unpaged();
            history = repository.listHistory(name);
        }
        var mappedHistory = history.stream().map(this::mapProjectRevision).collect(Collectors.toList());
        Collections.reverse(mappedHistory);
        if (pageable.isUnpaged()) {
            return new PageResponse<>(mappedHistory, -1, mappedHistory.size());
        } else {
            return new PageResponse<>(mappedHistory, pageable.getPageNumber(), pageable.getPageSize());
        }
    }

    private ProjectRevision mapProjectRevision(FileData src) {
        var userInfo = new UserInfoModel();
        var author = src.getAuthor();
        userInfo.setEmail(author != null ? author.getEmail() : null);
        userInfo.setDisplayName(author != null ? author.getName() : null);

        var revision = src.getVersion();
        if (revision == null || revision.isBlank()) {
            revision = "0";
        }
        var modifiedAt = src.getModifiedAt();
        var originalComment = src.getComment();
        var parts = commentService.getCommentParts(originalComment);

        return ProjectRevision.builder()
                .revisionNo(revision)
                .shortRevisionNo(revision.length() > SHORT_REVISION_LENGTH
                        ? revision.substring(0, SHORT_REVISION_LENGTH)
                        : null)
                .createdAt(modifiedAt == null ? null : DateTimes.atSystemZone(modifiedAt))
                .fullComment(originalComment)
                .author(userInfo)
                .deleted(src.isDeleted())
                .technicalRevision(src.isTechnicalRevision())
                .commentParts(parts.size() == COMMENT_PARTS ? parts : null)
                .build();
    }
}
