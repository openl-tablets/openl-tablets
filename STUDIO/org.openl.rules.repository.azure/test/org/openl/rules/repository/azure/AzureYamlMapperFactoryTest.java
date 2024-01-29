package org.openl.rules.repository.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.junit.jupiter.api.Test;

import org.openl.rules.dataformat.yaml.YamlMapperFactory;

public class AzureYamlMapperFactoryTest {

    private final YAMLMapper mapper = YamlMapperFactory.getYamlMapper();

    @Test
    public void testDeserialization() throws IOException {
        AzureCommit commit = null;
        try (var is = AzureYamlMapperFactoryTest.class.getResourceAsStream("/azureCommitOldStyle.yaml")) {
            commit = mapper.readValue(is, AzureCommit.class);
            assertTheSame(commit);
        }
        assertNotNull(commit);
        commit.setVersion("Foo");
        commit.setPath("Bar");
        assertTheSame(mapper.readValue(mapper.writeValueAsBytes(commit), AzureCommit.class));
    }

    public void assertTheSame(AzureCommit commit) {
        assertNull(commit.getVersion());
        assertNull(commit.getModifiedAt());
        assertNull(commit.getPath());
        assertFalse(commit.isDeleted());
        assertEquals("Edit rules/project1", commit.getComment());
        assertEquals("john_smith", commit.getAuthor());
        assertEquals(2, commit.getFiles().size());
        assertEquals("rules/project1/file11", commit.getFiles().get(0).getPath());
        assertEquals("38d9f188-7dd9-46ca-bc8d-dd351ae1c42c", commit.getFiles().get(0).getRevision());
        assertEquals("rules/project1/file12", commit.getFiles().get(1).getPath());
        assertEquals("a4cb4b68-783b-4485-958d-7782322f1449", commit.getFiles().get(1).getRevision());
    }

}
