package org.openl.studio.transfer.model;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * A transfer link just issued.
 *
 * @author Yury Molchan
 */
@Builder
@Schema(description = "A transfer link just issued")
public record TransferLinkView(
        @Parameter(description = """
                Address of the link relative to the address of OpenL Studio, such as \
                transfers/<id>. The id is the only credential a request to the link needs.""")
        String path,

        @Parameter(description = "When the link stops working.")
        Instant expiresAt) {
}
