package org.openl.studio.audit.model;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import org.openl.rules.repository.api.Pageable;
import org.openl.studio.common.model.PageResponse;

@Schema(description = "A page of the audit log, the newest events first.")
@Getter
public final class AuditEventsPage extends PageResponse<AuditEventView> {

    @Parameter(description = "Whether more events match than the total, which stops at 10000. Present only then.")
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private final boolean totalLimited;

    private AuditEventsPage(Collection<AuditEventView> content, Pageable page, Long total, boolean totalLimited) {
        super(content, page, total);
        this.totalLimited = totalLimited;
    }

    /**
     * Builds a page of events from the events already read, deriving the page number and size from the request.
     *
     * @param total        how many events the query matches, up to the most a page counts
     * @param totalLimited whether more events match than the total tells
     */
    public static AuditEventsPage page(Collection<AuditEventView> content,
                                       Pageable page,
                                       long total,
                                       boolean totalLimited) {
        return new AuditEventsPage(content, page, total, totalLimited);
    }
}
