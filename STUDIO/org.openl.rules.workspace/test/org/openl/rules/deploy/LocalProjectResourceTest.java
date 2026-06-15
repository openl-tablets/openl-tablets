package org.openl.rules.deploy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.openl.rules.common.ProjectException;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;

class LocalProjectResourceTest {

    private static FileData data(String name) {
        FileData fileData = new FileData();
        fileData.setName(name);
        return fileData;
    }

    @Test
    void constructionDoesNotOpenStream() {
        Repository repository = mock(Repository.class);
        new LocalProjectResource("rules.xml", repository, data("deploy/proj/rules.xml"));
        // Streams must be opened lazily: nothing is read until getContent() is called.
        verifyNoInteractions(repository);
    }

    @Test
    void getContentOpensFreshStreamEachCall() throws Exception {
        Repository repository = mock(Repository.class);
        FileData fileData = data("deploy/proj/rules.xml");
        when(repository.read("deploy/proj/rules.xml")).thenAnswer(
                inv -> new FileItem(fileData, new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8))));

        LocalProjectResource resource = new LocalProjectResource("rules.xml", repository, fileData);

        InputStream first;
        try (InputStream content = resource.getContent()) {
            first = content;
            assertEquals("hello", new String(content.readAllBytes(), StandardCharsets.UTF_8));
        }
        try (InputStream second = resource.getContent()) {
            assertNotSame(first, second);
            assertEquals("hello", new String(second.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void getContentThrowsWhenResourceMissing() throws Exception {
        Repository repository = mock(Repository.class);
        when(repository.read("deploy/proj/missing.xml")).thenReturn(null);

        LocalProjectResource resource = new LocalProjectResource("missing.xml",
                repository,
                data("deploy/proj/missing.xml"));
        assertThrows(ProjectException.class, resource::getContent);
    }

    @Test
    void getContentWrapsIOException() throws Exception {
        Repository repository = mock(Repository.class);
        when(repository.read("deploy/proj/rules.xml")).thenThrow(new IOException("boom"));

        LocalProjectResource resource = new LocalProjectResource("rules.xml",
                repository,
                data("deploy/proj/rules.xml"));
        ProjectException ex = assertThrows(ProjectException.class, resource::getContent);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void getFileDataReturnsData() {
        Repository repository = mock(Repository.class);
        FileData fileData = data("deploy/proj/rules.xml");
        LocalProjectResource resource = new LocalProjectResource("rules.xml", repository, fileData);
        assertSame(fileData, resource.getFileData());
    }
}
