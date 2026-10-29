package org.openl.studio.session;

/**
 * Hears of the clients {@link ClientSessions} starts and of the ones it releases after they were idle.
 *
 * <p>A client starts when its credentials make a request while nothing is kept for them: the first request, or the
 * first after the client was released. A release with the application, as at its restart or its stop, is not heard
 * here.
 */
public interface ClientSessionListener {

    /** A client started. It is heard on the request that started it. */
    void started(CredentialClient client);

    /** A client was released after its credentials made no request for the idle time of an HTTP session. */
    void timedOut(CredentialClient client);
}
