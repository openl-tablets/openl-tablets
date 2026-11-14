package org.openl.studio.repositories.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.aws.S3Repository;

class HistoryRepositoryMapperTest {

    @Test
    void shouldReturnHistoryWhenAuthorIsNull() throws IOException {
        FileData fileData = mock(FileData.class);
        when(fileData.getAuthor()).thenReturn(null);

        Repository repository = mock(S3Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        var history = List.of(fileData);
        when(repository.listHistory(anyString())).thenReturn(history);

        var historyRepositoryMapper = new HistoryRepositoryMapper(repository);

        var revisionHistory = historyRepositoryMapper.getProjectHistory("name",
                "filter",
                false,
                Pageable.unpaged());
        assertEquals(history.size(), revisionHistory.getContent().size());
        assertNull(revisionHistory.getTotal());
    }

    @Test
    void shouldExposeCreatedAtWithSystemZone() throws IOException {
        var modifiedAt = Date.from(Instant.parse("2024-01-02T03:04:05Z"));
        FileData fileData = mock(FileData.class);
        when(fileData.getAuthor()).thenReturn(null);
        when(fileData.getModifiedAt()).thenReturn(modifiedAt);

        Repository repository = mock(S3Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        when(repository.listHistory(anyString())).thenReturn(List.of(fileData));

        var historyRepositoryMapper = new HistoryRepositoryMapper(repository);

        var revisionHistory = historyRepositoryMapper.getProjectHistory("name",
                "filter",
                false,
                Pageable.unpaged());
        var createdAt = revisionHistory.getContent().iterator().next().createdAt();

        assertEquals(modifiedAt.toInstant(), createdAt.toInstant());
        assertEquals(ZoneId.systemDefault(), createdAt.getZone());
    }

    @Test
    void splitsTheCommentOfACopyAroundTheSourceProject() {
        var copy = new FileData();
        copy.setVersion("2");
        copy.setComment("Copied from: Bank Rating.");
        var other = new FileData();
        other.setVersion("1");
        other.setComment("Project Bank Rating is saved.");
        var mapper = new HistoryRepositoryMapper(mock(Repository.class));

        assertEquals(List.of("Copied from: ", "Bank Rating", "."), mapper.revisionOf(copy).commentParts());
        assertNull(mapper.revisionOf(other).commentParts());
    }
}
