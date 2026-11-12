package org.openl.studio.security.saml;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import org.junit.jupiter.api.Test;

/**
 * Checks that reading the SAML metadata of a server that never answers ends, instead of holding the start.
 *
 * @author Yury Molchan
 */
class SamlMetadataTimeoutTest {

    @Test
    void givesUpOnAServerThatAcceptsTheConnectionAndNeverAnswers() throws IOException {
        // The backlog accepts the connection; nothing ever reads the request or writes an answer.
        try (var silentServer = new ServerSocket(0)) {
            var location = "http://localhost:" + silentServer.getLocalPort() + "/saml/metadata";

            var error = assertTimeoutPreemptively(Duration.ofSeconds(20),
                    () -> assertThrows(IOException.class,
                            () -> LazyInMemoryRelyingPartyRegistrationRepository
                                    .fromMetadata(location, Duration.ofSeconds(2), Duration.ofMillis(300))));

            assertTrue(error.getMessage().contains(location), error.getMessage());
        }
    }
}
