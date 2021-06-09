package org.openl.rules.repository.git;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotResettableCredentialsProvider extends UsernamePasswordCredentialsProvider {
    private static final Logger LOG = LoggerFactory.getLogger(NotResettableCredentialsProvider.class);

    private static final String FAIL_MESSAGE = "Problem communicating with '%s' Git server, will retry automatically in %s";
    private static final String BLOCK_MESSAGE = "Problem communicating with '%s' Git server, please contact admin.";
    private static final String INCORRECT_CRED_MESSAGE = "Incorrect login or password for '%s' Git repository.";


    private final int failedAuthorizationSeconds;
    private final Integer maxAuthorizationAttempts;
    private final String repositoryName;

    //When authentication attempt was unsuccessful the next authentication attempt does not occur immediately,
    //but after the time specified in the properties.
    private AtomicLong nextAttempt = new AtomicLong(0);
    private AtomicInteger failedAttempts = new AtomicInteger(0);

    private final Set<GitActionType> currentActions = new HashSet<>();
    private final Set<GitActionType> failedActions = new HashSet<>();


    NotResettableCredentialsProvider(String username, String password, String repositoryName, int failedAuthorizationSeconds, Integer maxAuthorizationAttempts) {
        super(username, password);
        this.repositoryName = repositoryName;
        this.failedAuthorizationSeconds = failedAuthorizationSeconds;
        this.maxAuthorizationAttempts = maxAuthorizationAttempts;
        currentActions.add(GitActionType.INIT);
    }

    @Override
    public void reset(URIish uri) {
        // This method is called when authentication attempt was unsuccessful and need to provide correct credentials.
        // Our application works in non-interactive mode so we just throw exception.
        LOG.info("Reset the credentials provider for the URI: {}", uri);
        synchronized (this) {
            failedActions.addAll(currentActions);
        }
        if (currentActions.contains(GitActionType.INIT)) {
            throw new InvalidCredentialsException(String.format(INCORRECT_CRED_MESSAGE, repositoryName));
        }
        if (maxAuthorizationAttempts != null && failedAttempts.incrementAndGet() >= maxAuthorizationAttempts) {
            // The maximum number of authorization attempts has been exceeded. No more attempts allowed.
            nextAttempt.set(-1);
            throw new InvalidCredentialsException(String.format(BLOCK_MESSAGE, repositoryName));
        }
        nextAttempt.set(System.currentTimeMillis() + failedAuthorizationSeconds * 1000L);
        throw new InvalidCredentialsException(String.format(FAIL_MESSAGE, repositoryName, getNextAttemptTime()));
    }

    void validateAuthorizationState(GitActionType actionType) throws InvalidCredentialsException {
        long attemptTime = nextAttempt.get();
        if (attemptTime == 0) {
            // The last login attempt was successful, or this is the first attempt.
            authTaken(actionType);
            return;
        } else if (attemptTime == -1) {
            // The maximum number of authorization attempts has been exceeded.
            throw new InvalidCredentialsException(String.format(BLOCK_MESSAGE, repositoryName));
        } else {
            if (System.currentTimeMillis() > attemptTime) {
                if (maxAuthorizationAttempts == null || failedAttempts.get() <= maxAuthorizationAttempts) {
                    authTaken(actionType);
                    return;
                } else {
                    // The maximum number of authorization attempts has been exceeded. No more attempts allowed.
                    nextAttempt.set(-1);
                    throw new InvalidCredentialsException(String.format(BLOCK_MESSAGE, repositoryName));
                }
            } else {
                // The time for the next try has not yet come
                throw new InvalidCredentialsException(String.format(FAIL_MESSAGE, repositoryName, getNextAttemptTime()));
            }
        }
    }

    private String getNextAttemptTime() {
        int nextAttemptTimeInSeconds = (int) (nextAttempt.get() - System.currentTimeMillis()) / 1000;
        int minutes = nextAttemptTimeInSeconds / 60;
        if (minutes != 0) {
            return minutes + " minute(s).";
        } else {
            return nextAttemptTimeInSeconds + " second(s).";
        }
    }

    private synchronized void authTaken(GitActionType actionType) {
        if (actionType != null) {
            currentActions.add(actionType);
        }
    }

    void successAuthentication(GitActionType actionType) {
        synchronized (this) {
            currentActions.remove(actionType);
            failedActions.remove(actionType);
        }
        if (failedActions.isEmpty()) {
            nextAttempt = new AtomicLong(0);
            failedAttempts = new AtomicInteger(0);
        }
    }

    boolean isHasAuthorizationFailure() {
        return !failedActions.isEmpty();
    }


    @Override
    public void clear() {
        // Do nothing to ensure that username and password isn't cleared.
        LOG.warn("clear() method should never be invoked.");
    }
}
