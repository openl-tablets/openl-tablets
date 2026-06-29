package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.file.FileSystemRepository;

class MappedRepositoryTest {

    @TempDir
    Path root;

    /**
     * A project whose rules.xml has a blank name must be mapped under its folder name.
     *
     * An empty business name later resolves the local workspace path to "/", which escapes the
     * repository root on open. Such a descriptor cannot be produced through the UI, only on disk,
     * so the fallback is verified at the repository level. See {@code ProjectDescriptor#fillProjectName}.
     */
    @Test
    void blankRulesXmlNameFallsBackToFolderName() throws IOException {
        writeProject("no-name-project", "<project><name></name></project>");
        writeProject("absent-name-project", "<project></project>");
        writeProject("named-project", "<project><name>RealName</name></project>");

        List<String> mapped = listMappedFolders("DESIGN/");

        assertEquals(3, mapped.size(), "All projects must be mapped: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/no-name-project:")),
                "Blank rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/absent-name-project:")),
                "Missing rules.xml name must fall back to the folder name, but was: " + mapped);
        assertTrue(mapped.stream().anyMatch(name -> name.startsWith("DESIGN/RealName:")),
                "Named project must keep its rules.xml name, but was: " + mapped);
    }

    private List<String> listMappedFolders(String baseFolder) throws IOException {
        FileSystemRepository delegate = new FileSystemRepository();
        delegate.setRoot(root);
        delegate.initialize();

        Repository mapped = MappedRepository.create(delegate, baseFolder);
        try {
            return mapped.listFolders(baseFolder).stream().map(FileData::getName).toList();
        } finally {
            if (mapped instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }

    private void writeProject(String folder, String rulesXml) throws IOException {
        Path projectFolder = root.resolve(folder);
        Files.createDirectories(projectFolder);
        Files.writeString(projectFolder.resolve("rules.xml"), rulesXml, StandardCharsets.UTF_8);
    }

}
