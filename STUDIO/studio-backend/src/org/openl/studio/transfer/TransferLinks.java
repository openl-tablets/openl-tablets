package org.openl.studio.transfer;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.util.HashingUtils;

/**
 * The transfer links that wait to be used.
 *
 * <p>A link is named by a random id of 256 bits, which is its only credential. The links are kept under the hash of
 * their ids, so what is kept here cannot be used as a link.
 *
 * <p>A link stops working when it expires, when it is spent — an upload by its first request — and when the state
 * of the client that issued it ends ({@link ClientTransferLinks}). The links live in memory, on one node.
 *
 * @author Yury Molchan
 */
@Component
public class TransferLinks {

    private static final int ID_BYTES = 32;

    private final Map<String, TransferLink> links = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;
    private final Duration lifetime;

    /** How many links one client holds at a time, one at least; a new one beyond it pushes out the oldest. */
    @Getter
    private final int maxPending;

    public TransferLinks(Clock clock,
                         @Value("${transfer.link.ttl}") String lifetime,
                         @Value("${transfer.max-pending}") int maxPending) {
        this.clock = clock;
        this.lifetime = Duration.parse(lifetime);
        // Fewer than one would push out the very link a client has just been issued
        this.maxPending = Math.max(1, maxPending);
    }

    /**
     * Keeps a new link that runs the operation as the user, for the client kept under the given key.
     *
     * @return the id of the link and the key it is kept under
     */
    Issued add(String user, String clientKey, TransferOperation operation) {
        var bytes = new byte[ID_BYTES];
        random.nextBytes(bytes);
        var id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var link = new TransferLink(user, clientKey, operation, clock.instant().plus(lifetime));
        var key = HashingUtils.sha256Hex(id);
        links.put(key, link);
        return new Issued(id, key, link);
    }

    /**
     * The link of the given id, as long as it works. A link of a download stays, for the next request.
     */
    public @Nullable TransferLink find(String id) {
        return byKey(HashingUtils.sha256Hex(id));
    }

    /**
     * The link of an upload of the given id, taken away so that no other request finds it, as long as it works. A link
     * of a download is left as it is.
     */
    public @Nullable TransferLink takeUpload(String id) {
        var key = HashingUtils.sha256Hex(id);
        var link = byKey(key);
        return link != null && link.operation().once() && links.remove(key, link) ? link : null;
    }

    /** Whether the link kept under the given key still works. */
    boolean isPending(String key) {
        return byKey(key) != null;
    }

    /** Drops the links kept under the given keys. */
    void remove(Collection<String> keys) {
        keys.forEach(links::remove);
    }

    /** How many links are kept. */
    int size() {
        return links.size();
    }

    /** The link kept under the given key, as long as it works; an expired one is dropped. */
    private @Nullable TransferLink byKey(String key) {
        var link = links.get(key);
        if (link != null && link.isExpired(clock.instant())) {
            links.remove(key, link);
            return null;
        }
        return link;
    }

    /**
     * A link just issued.
     *
     * @param id   the id the link is used by
     * @param key  the key the link is kept under
     * @param link the link
     */
    public record Issued(String id, String key, TransferLink link) {
    }
}
