package org.openl.rules.repository.api;

import java.util.Date;

/**
 * Global history DTO
 *
 * @author Vladyslav Pikus
 */
public class HistoryLog {

    private final String id;
    private final String fullCommit;
    private final UserInfo author;
    private final Date modifiedAt;

    public HistoryLog(String id, String fullCommit, UserInfo author, Date modifiedAt) {
        this.id = id;
        this.fullCommit = fullCommit;
        this.author = author;
        this.modifiedAt = modifiedAt;
    }

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
