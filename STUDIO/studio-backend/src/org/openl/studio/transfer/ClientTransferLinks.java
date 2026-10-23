package org.openl.studio.transfer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.studio.session.ClientSessionScope;

/**
 * The transfer links one client has issued, which end with the state kept for the client.
 *
 * <p>A client holds at most {@link TransferLinks#getMaxPending()} links: a new one beyond it pushes out the oldest,
 * so the links a client abandoned never keep it from issuing new ones.
 *
 * @author Yury Molchan
 */
@Component
@ClientSessionScope
@RequiredArgsConstructor
public class ClientTransferLinks {

    private final TransferLinks links;
    private final Deque<String> issued = new ArrayDeque<>();

    /**
     * Issues a link that runs the operation as the user, for the client kept under the given key.
     */
    public synchronized TransferLinks.Issued issue(String user, String clientKey, TransferOperation operation) {
        issued.removeIf(key -> !links.isPending(key));
        var link = links.add(user, clientKey, operation);
        issued.addLast(link.key());
        var evicted = new ArrayList<String>();
        while (issued.size() > links.getMaxPending()) {
            evicted.add(issued.removeFirst());
        }
        links.remove(evicted);
        return link;
    }

    /** Ends every link of the client, as the state kept for the client ends. */
    @PreDestroy
    public synchronized void end() {
        links.remove(issued);
        issued.clear();
    }
}
