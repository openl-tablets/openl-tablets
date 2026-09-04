package org.openl.rules.repository.api;

import java.util.Date;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Global history DTO
 *
 * @author Vladyslav Pikus
 */
@RequiredArgsConstructor
public class HistoryLog {

    @Getter
    private final String id;
    @Getter
    private final String fullCommit;
    @Getter
    private final UserInfo author;
    @Getter
    private final Date modifiedAt;
}
