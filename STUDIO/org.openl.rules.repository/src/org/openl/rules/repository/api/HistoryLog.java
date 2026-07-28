package org.openl.rules.repository.api;

import java.util.Date;

import lombok.RequiredArgsConstructor;

/**
 * Global history DTO
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class HistoryLog {

    private final String id;
    private final String fullCommit;
    private final UserInfo author;
    private final Date modifiedAt;

    public String getId() {
        return id;
    }

    public UserInfo getAuthor() {
        return author;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public String getFullCommit() {
        return fullCommit;
    }
}
