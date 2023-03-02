package org.openl.rules.repository.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.openl.rules.dataformat.yaml.YamlMapperFactory;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

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
