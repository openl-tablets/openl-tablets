package org.openl.studio.security.pat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class Base62GeneratorTest {

    // Must match Base62Generator alphabet:
    private static final String BASE62_REGEX = "^[0-9a-zA-Z]+$";

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 8, 16, 24, 32, 64})
    @DisplayName("generate(length) returns a non-null string with exact length")
    void generate_returnsStringWithExactLength(int length) {
        String s = Base62Generator.generate(length);
        assertNotNull(s);
        assertEquals(length, s.length());
    }

    @Test
    @DisplayName("generate(length) uses only Base62 characters")
    void generate_usesOnlyBase62Chars() {
        String s = Base62Generator.generate(256);
        assertTrue(s.matches(BASE62_REGEX), "Generated string contains non-base62 characters: " + s);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("generate(length) throws IllegalArgumentException for non-positive length")
    void generate_throwsForInvalidLength(int length) {
        assertThrows(IllegalArgumentException.class, () -> Base62Generator.generate(length));
    }

    @Test
    @DisplayName("generate(length) produces sufficiently unique values across many samples (probabilistic)")
    void generate_uniquenessProbabilistic() {
        // This is a probabilistic test. With length=16 base62 (~95 bits), collisions in 10k samples
        // are astronomically unlikely. If this ever flakes, something is seriously wrong (or RNG is broken).
        var samples = 10_000;
        var length = 16;

        var seen = new HashSet<String>(samples * 2);
        for (var i = 0; i < samples; i++) {
            String s = Base62Generator.generate(length);
            assertTrue(seen.add(s), "Collision detected at sample " + i + ": " + s);
        }
    }

    @Test
    @DisplayName("generate(length) is safe under concurrency (no exceptions, correct format)")
    void generate_concurrentUsage() throws Exception {
        var threads = Math.max(4, Runtime.getRuntime().availableProcessors());
        var tasks = 20_000;
        var length = 24;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            var cs = new ExecutorCompletionService<String>(pool);
            for (var i = 0; i < tasks; i++) {
                cs.submit(() -> Base62Generator.generate(length));
            }

            for (var i = 0; i < tasks; i++) {
                var s = cs.take().get(10, TimeUnit.SECONDS);
                assertNotNull(s);
                assertEquals(length, s.length());
                assertTrue(s.matches(BASE62_REGEX), "Generated string contains non-base62 characters: " + s);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
