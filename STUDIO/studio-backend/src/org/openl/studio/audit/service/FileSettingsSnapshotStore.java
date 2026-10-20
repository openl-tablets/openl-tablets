package org.openl.studio.audit.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.openl.util.PropertiesUtils;

/**
 * Keeps the snapshot of the settings in a file of the home folder of this instance of OpenL Studio, which outlives
 * its restarts as the clones of the repositories do.
 *
 * <p>The file is replaced as a whole, so a start stopped midway leaves the snapshot of the last start. It lists the
 * settings in the order of their names.
 */
@Component
@RequiredArgsConstructor
public class FileSettingsSnapshotStore implements SettingsSnapshotStore {

    static final String FILE_NAME = "settings-snapshot.properties";

    @Value("${openl.home}")
    private final String home;

    @Override
    public Optional<Map<String, String>> load() throws IOException {
        var file = file();
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        var snapshot = new TreeMap<String, String>();
        PropertiesUtils.load(file, snapshot::put);
        return Optional.of(snapshot);
    }

    @Override
    public void save(Map<String, String> snapshot) throws IOException {
        var file = file();
        Files.createDirectories(file.getParent());
        var written = Files.createTempFile(file.getParent(), FILE_NAME, ".tmp");
        try {
            PropertiesUtils.store(written, new TreeMap<>(snapshot).entrySet());
            Files.move(written, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(written);
        }
    }

    private Path file() {
        return Path.of(home, FILE_NAME);
    }
}
