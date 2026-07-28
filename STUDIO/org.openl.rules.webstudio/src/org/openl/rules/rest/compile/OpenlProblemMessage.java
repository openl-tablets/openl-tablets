package org.openl.rules.rest.compile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.openl.message.Severity;

@RequiredArgsConstructor
public class OpenlProblemMessage {

    @Getter
    private final long id;
    @Getter
    private final String summary;
    @Getter
    private final boolean hasStacktrace;
    @Getter
    private final String[] errorCode;
    @Getter
    private final boolean hasLinkToCell;
    @Getter
    private final String tableId;
    @Getter
    private final String errorCell;
    @Getter
    private final Severity severity;
}
