package org.openl.rules.webstudio.web.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import org.openl.rules.repository.RepositoryMode;

/**
 * Creating a repository reaches its storage, so a repository that refuses must not be reached again by every
 * caller behind it. Listing projects asks whether the user may deploy anywhere, and one unreachable production
 * repository used to make every listing wait for its connection to give up.
 */
class DefaultRepositoryFactoryProxyTest {

    private static final String CONFIG = "production";

    private MockEnvironment environmentWith(String factory) {
        var environment = new MockEnvironment();
        environment.setProperty("repository." + CONFIG + ".factory", factory);
        return environment;
    }

    @Test
    void aRefusedRepositoryIsNotReachedAgainByTheNextCaller() {
        // A factory nothing accepts refuses without ever touching a storage, which is what a broken
        // configuration does too, only faster.
        var environment = environmentWith("repo-does-not-exist");
        var attempts = new AtomicInteger();
        var proxy = new DefaultRepositoryFactoryProxy(new CountingEnvironment(environment, attempts),
                RepositoryMode.PRODUCTION);

        var first = assertThrows(RuntimeException.class, () -> proxy.getRepositoryInstance(CONFIG));
        var second = assertThrows(RuntimeException.class, () -> proxy.getRepositoryInstance(CONFIG));

        assertEquals(1, attempts.get(), "the repository must be reached once, not once per caller");
        // Each caller is refused by an exception of its own: the first one belongs to the request that
        // reached the storage and would point every later log entry at an unrelated call site.
        assertNotSame(first, second);
        assertSame(first, second.getCause());
    }

    @Test
    void aRepositoryIsReachedAgainAfterItsConfigurationIsReleased() {
        var environment = environmentWith("repo-does-not-exist");
        var attempts = new AtomicInteger();
        var proxy = new DefaultRepositoryFactoryProxy(new CountingEnvironment(environment, attempts),
                RepositoryMode.PRODUCTION);

        assertThrows(RuntimeException.class, () -> proxy.getRepositoryInstance(CONFIG));
        proxy.releaseRepository(CONFIG);
        assertThrows(RuntimeException.class, () -> proxy.getRepositoryInstance(CONFIG));

        assertEquals(2, attempts.get(), "releasing the repository forgets that it refused");
    }

    @Test
    void aRepositoryThatWasBuiltIsHandedOutAgainWithoutBuildingIt(@TempDir Path root) {
        var environment = environmentWith("repo-file");
        environment.setProperty("repository." + CONFIG + ".uri", root.toString());
        var attempts = new AtomicInteger();
        var proxy = new DefaultRepositoryFactoryProxy(new CountingEnvironment(environment, attempts),
                RepositoryMode.PRODUCTION);

        var first = proxy.getRepositoryInstance(CONFIG);
        var second = proxy.getRepositoryInstance(CONFIG);

        assertNotNull(first);
        assertSame(first, second);
        assertEquals(1, attempts.get(), "a repository that answered is built once");

        proxy.destroy();
        proxy.getRepositoryInstance(CONFIG);
        assertEquals(2, attempts.get(), "destroying the proxy forgets the repositories it built");
    }

    /** Counts how many times a repository was actually built from its configuration. */
    private static final class CountingEnvironment extends org.springframework.core.env.AbstractEnvironment {
        private final MockEnvironment delegate;
        private final AtomicInteger attempts;

        private CountingEnvironment(MockEnvironment delegate, AtomicInteger attempts) {
            this.delegate = delegate;
            this.attempts = attempts;
        }

        @Override
        public String getProperty(String key) {
            if (key.endsWith(".factory")) {
                attempts.incrementAndGet();
            }
            return delegate.getProperty(key);
        }
    }
}
