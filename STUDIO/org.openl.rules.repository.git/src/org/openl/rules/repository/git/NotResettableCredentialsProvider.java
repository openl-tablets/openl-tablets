package org.openl.rules.repository.git;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class NotResettableCredentialsProvider extends UsernamePasswordCredentialsProvider {

    private final int failedAuthorizationSeconds;
    private final int maxAuthorizationAttempts;

    //When authentication attempt was unsuccessful the next authentication attempt does not occur immediately,
    //but after the time specified in the properties.
    private AtomicLong nextAttempt = new AtomicLong(0);
    private AtomicInteger attemptNumber = new AtomicInteger(0);


    NotResettableCredentialsProvider(String username, String password, int failedAuthorizationSeconds, int maxAuthorizationAttempts) {
        super(username, password);
        this.failedAuthorizationSeconds = failedAuthorizationSeconds;
        this.maxAuthorizationAttempts = maxAuthorizationAttempts;
    }

    @Override
    public void reset(URIish uri) {
        // This method is called when authentication attempt was unsuccessful and need to provide correct credentials.
        // Our application works in non-interactive mode so we just throw exception.
        if (!nextAttempt.compareAndSet(0, System.currentTimeMillis() + failedAuthorizationSeconds * 1000)) {
            // The following condition will be false only in case of a simultaneous request to the repository by several threads
            // It is necessary to increase the attempt counter so that the total number of attempts does not exceed the maximum number of attempts.
            if (!attemptNumber.compareAndSet(0, 1)) {
                //Means that the one more authentication attempt has failed
                nextAttempt.set(System.currentTimeMillis() + failedAuthorizationSeconds * 1000);
            }
        }
        throw new InvalidCredentialsException(String.format("Problem communicating with Git server, will retry automatically in %s", getNextAttemptTime()));
    }

    void validateAuthorizationState() throws IOException {
        long attemptTime = nextAttempt.get();
        if (attemptTime == 0) {
            // The last login attempt was successful, or this is the first attempt.
            return;
        } else if (attemptTime == -1) {
            // The maximum number of authorization attempts has been exceeded.
            throw new IOException("Incorrect login or password for git repository.");
        } else {
            if (System.currentTimeMillis() > attemptTime) {
                if (attemptNumber.incrementAndGet() <= maxAuthorizationAttempts) {
                    // Increase in the counter of attempts and permission for one more.
                    return;
                } else {
                    // The maximum number of authorization attempts has been exceeded. No more attempts allowed.
                    nextAttempt.set(-1);
                    throw new IOException("Incorrect login or password for git repository.");
                }
            } else {
                // The time for the next try has not yet come
                throw new IOException(String.format("Problem communicating with Git server, will retry automatically in %s", getNextAttemptTime()));
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

    void successAuthentication() {
        nextAttempt = new AtomicLong(0);
        attemptNumber = new AtomicInteger(0);
    }
}
