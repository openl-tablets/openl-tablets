package org.openl.rules.webstudio.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import org.openl.config.InMemoryProperties;
import org.openl.spring.env.RefPropertySource;
import org.openl.studio.audit.service.SettingsAuditor;

class SettingsServiceImplTest {

    /** The settings saved, read before the defaults as the settings file is. */
    private final Map<String, Object> saved = new HashMap<>(Map.of("user.mode", "single", "mail.username", "bot"));
    private final SettingsAuditor auditor = mock(SettingsAuditor.class);
    private final StandardEnvironment environment = environment();
    private final InMemoryProperties properties = new InMemoryProperties(environment);
    private final SettingsServiceImpl service = new SettingsServiceImpl(properties, environment, auditor, settings ->
            settings.forEach((property, value) -> {
                if (value == null) {
                    saved.remove(property);
                } else {
                    saved.put(property, value);
                }
            }));

    @Test
    void announcesTheSettingsOfASaveTogetherWithTheirValuesBeforeAndAfter() throws Exception {
        properties.setProperty("user.mode", "multi");
        properties.setProperty("project.history.count", "50");

        service.commit();

        assertEquals("multi", environment.getProperty("user.mode"));
        verify(auditor).changed(Map.of("project.history.count", "100", "user.mode", "single"),
                Map.of("project.history.count", "50", "user.mode", "multi"));
    }

    @Test
    void announcesASettingRevertedWithTheDefaultItTakes() throws Exception {
        properties.revertProperties("mail.username");

        service.commit();

        verify(auditor).changed(Map.of("mail.username", "bot"), Map.of("mail.username", "admin"));
    }

    @Test
    void announcesAValueAsItIsWrittenNotTheValueOfTheVariableItRefersTo() throws Exception {
        environment.getPropertySources().addFirst(new MapPropertySource("environment variables",
                Map.of("SMTP_ADDRESS", "smtp://bot:secret@mail.example.com")));
        properties.setProperty("mail.url", "${SMTP_ADDRESS}");
        // Saved meanwhile, as another administrator may do.
        saved.put("mail.url", "${missing.property}");

        service.commit();

        assertEquals("smtp://bot:secret@mail.example.com", environment.getProperty("mail.url"));
        verify(auditor).changed(Map.of("mail.url", "${missing.property}"), Map.of("mail.url", "${SMTP_ADDRESS}"));
    }

    @Test
    void savesASettingWhoseDefaultCannotBeRead() throws Exception {
        saved.put("repository.design.uri", "https://git.example.com/rules.git");
        environment.getPropertySources().addLast(new MapPropertySource("references",
                Map.of("repository.design.$ref", "repo-git", "repo-git.uri", "${missing.property}")));
        environment.getPropertySources().addLast(new RefPropertySource(environment, environment.getPropertySources()));
        properties.revertProperties("repository.design.uri");

        service.commit();

        assertFalse(saved.containsKey("repository.design.uri"));
        verify(auditor).changed(Map.of("repository.design.uri", "https://git.example.com/rules.git"),
                Collections.singletonMap("repository.design.uri", null));
    }

    @Test
    void savesTheSettingsOneSaveAtATime() throws Exception {
        var saving = new CountDownLatch(1);
        var released = new CountDownLatch(1);
        var saves = new AtomicInteger();
        var slow = new SettingsServiceImpl(properties, environment, auditor, settings -> {
            saves.incrementAndGet();
            saving.countDown();
            awaitQuietly(released);
        });
        var quick = new SettingsServiceImpl(properties, environment, auditor, settings -> saves.incrementAndGet());

        var first = Thread.ofVirtual().start(() -> commit(slow));
        saving.await();
        var second = Thread.ofVirtual().start(() -> commit(quick));

        assertFalse(second.join(Duration.ofMillis(300)), "The second save waits for the first one");
        assertEquals(1, saves.get());
        released.countDown();
        assertTrue(first.join(Duration.ofSeconds(5)));
        assertTrue(second.join(Duration.ofSeconds(5)));
        assertEquals(2, saves.get());
    }

    @Test
    void hasNoSettingToAnnounceWhenOnlyTheTimeOfTheSaveChanged() throws Exception {
        service.commit();

        verify(auditor).changed(Map.of(), Map.of());
    }

    private static void commit(SettingsServiceImpl settings) {
        try {
            settings.commit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private StandardEnvironment environment() {
        var standard = new StandardEnvironment();
        standard.getPropertySources().addFirst(new MapPropertySource("saved", saved));
        standard.getPropertySources().addLast(new MapPropertySource("defaults",
                Map.of("user.mode", "single", "project.history.count", "100", "mail.username", "admin")));
        return standard;
    }
}
