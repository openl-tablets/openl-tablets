package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.openl.rules.webstudio.web.admin.RepositoryValidators.getMostSpecificMessage;


import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import javax.security.auth.login.FailedLoginException;

import org.junit.jupiter.api.Test;

public class RepositoryValidatorsTest {

    @Test
    public void testCommonExceptionMessages() {
        assertEquals("Invalid login or password. Try again.", getMostSpecificMessage(new IllegalArgumentException("Error:", new FailedLoginException())));
        assertEquals("Connection refused. Check the repository URL and try again.", getMostSpecificMessage(new IllegalArgumentException("Error:", new ConnectException())));

        IllegalArgumentException e1 = new IllegalArgumentException("Unable to execute HTTP request: test-bucket.someunserver.com", new UnknownHostException("test-bucket.someunserver.com"));
        assertEquals("Unknown host (test-bucket.someunserver.com).", getMostSpecificMessage(e1));

        IllegalArgumentException e2 = new IllegalArgumentException("Error:", new UnknownHostException());
        assertEquals("Unknown host.", getMostSpecificMessage(e2));
    }

    @Test
    public void testEmptyMessages() {
        IllegalArgumentException emptyRootCause = new IllegalArgumentException("Error:", new IllegalStateException("Incorrect URL.", new NullPointerException()));
        assertEquals("Incorrect URL.", getMostSpecificMessage(emptyRootCause));

        IllegalArgumentException longStackTrace = new IllegalArgumentException("Error:", new IOException("Internal error: ", new IllegalStateException("Incorrect URL.", new IOException("", new NullPointerException()))));
        assertEquals("Incorrect URL.", getMostSpecificMessage(longStackTrace));
    }
}