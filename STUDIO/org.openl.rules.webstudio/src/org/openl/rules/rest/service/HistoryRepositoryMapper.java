package org.openl.rules.rest.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.HistoryLog;
import org.openl.rules.repository.api.Page;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.api.SearchableRepository;
import org.openl.rules.rest.model.HistoryLogModel;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.openl.rules.rest.model.UserInfoModel;

public class HistoryRepositoryMapper {

    private final Repository repository;
    private final Comments commentService;

    public HistoryRepositoryMapper(Repository repository, Comments commentService) {
        this.repository = repository;
        this.commentService = commentService;
    }

    public PageResponse<HistoryLogModel> getOverallHistory(String globalFilter, Page page) throws IOException {
        if (!repository.supports().searchable()) {
            throw new IllegalStateException("Target repository doesn't support this feature.");
        }
        List<HistoryLogModel> history = ((SearchableRepository) repository).globalHistory(globalFilter, page)
            .stream()
            .map(this::mapHistoryLog)
            .collect(Collectors.toList());
        return new PageResponse<>(history, page.getPageNumber(), page.getPageSize());
    }

    private HistoryLogModel mapHistoryLog(HistoryLog src) {
        HistoryLogModel dest = new HistoryLogModel();
        String revision = src.getId();
        if (revision == null || revision.isBlank()) {
            revision = "0";
        }
        dest.setRevisionNo(revision);
        if (revision.length() > 6) {
            dest.setShortRevisionNo(revision.substring(0, 6));
        }
        dest.setCreatedAt(src.getModifiedAt());
        dest.setFullComment(src.getFullCommit());

        UserInfoModel userInfo = new UserInfoModel();
        userInfo.setEmail(src.getAuthor().getEmail());
        userInfo.setDisplayName(src.getAuthor().getDisplayName());
        dest.setAuthor(userInfo);

        return dest;
    }

    public PageResponse<ProjectRevision> getProjectHistory(String name,
            String globalFilter,
            Page page) throws IOException {
        List<FileData> history;
        if (repository.supports().searchable()) {
            history = ((SearchableRepository) repository).listHistory(name, globalFilter, page);
        } else {
            page = Page.unpaged();
            history = repository.listHistory(name);
        }
        var mappedHistory = history.stream().map(this::mapProjectRevision).collect(Collectors.toList());
        Collections.reverse(mappedHistory);
        if (page.isUnpaged()) {
            return new PageResponse<>(mappedHistory, -1, mappedHistory.size());
        } else {
            return new PageResponse<>(mappedHistory, page.getPageNumber(), page.getPageSize());
        }
    }

    private ProjectRevision mapProjectRevision(FileData src) {
        ProjectRevision dest = new ProjectRevision();
        UserInfoModel userInfo = new UserInfoModel();
        userInfo.setEmail(src.getAuthor().getEmail());
        userInfo.setDisplayName(src.getAuthor().getName());
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

        String originalComment = src.getComment();
        dest.setFullComment(originalComment);
        var parts = commentService.getCommentParts(originalComment);
        if (parts.size() == 3) {
            dest.setCommentParts(parts);
        }

        return dest;
    }
}
