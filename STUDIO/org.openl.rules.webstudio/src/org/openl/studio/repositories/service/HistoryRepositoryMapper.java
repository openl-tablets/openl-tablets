package org.openl.studio.repositories.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.rest.model.UserInfoModel;
import org.openl.studio.common.model.PageResponse;
import org.openl.studio.repositories.model.ProjectRevision;

/**
 * {@link Repository} history mapper
 *
 * @author Vladyslav Pikus
 */
public class HistoryRepositoryMapper {

    private final Repository repository;
    private final Comments commentService;

    public HistoryRepositoryMapper(Repository repository, Comments commentService) {
        this.repository = repository;
        this.commentService = commentService;
    }

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
        ProjectRevision dest = new ProjectRevision();
        UserInfoModel userInfo = new UserInfoModel();
        UserInfo author = src.getAuthor();
        userInfo.setEmail(author != null ? author.getEmail() : null);
        userInfo.setDisplayName(author != null ? author.getName() : null);
        dest.setAuthor(userInfo);
        dest.setCreatedAt(src.getModifiedAt());
        String revision = src.getVersion();
        if (revision == null || revision.isBlank()) {
            revision = "0";
        }
        dest.setRevisionNo(revision);
        if (revision.length() > 6) {
            dest.setShortRevisionNo(revision.substring(0, 6));
        }
        dest.setDeleted(src.isDeleted());
        dest.setTechnicalRevision(src.isTechnicalRevision());

        String originalComment = src.getComment();
        dest.setFullComment(originalComment);
        var parts = commentService.getCommentParts(originalComment);
        if (parts.size() == 3) {
            dest.setCommentParts(parts);
        }

        return dest;
    }
}
