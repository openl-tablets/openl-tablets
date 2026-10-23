package org.openl.studio.transfer;

import java.time.Instant;

/**
 * A transfer link waiting to be used.
 *
 * @param user      the user the operation runs as
 * @param clientKey the key the state of the client that issued the link is kept under
 * @param operation what the link does
 * @param expiresAt when the link stops working
 * @author Yury Molchan
 */
public record TransferLink(String user, String clientKey, TransferOperation operation, Instant expiresAt) {

    /** Whether the link no longer works at the given moment. */
    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
