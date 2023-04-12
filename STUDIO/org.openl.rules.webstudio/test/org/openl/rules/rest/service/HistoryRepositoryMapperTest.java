package org.openl.rules.rest.service;

import org.junit.Assert;
import org.junit.Test;
import org.openl.rules.project.abstraction.Comments;
import org.openl.rules.repository.api.FeaturesBuilder;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Pageable;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.aws.S3Repository;
import org.openl.rules.rest.model.PageResponse;
import org.openl.rules.rest.model.ProjectRevision;
import org.springframework.core.env.PropertyResolver;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HistoryRepositoryMapperTest {

    @Test
    public void shouldReturnHistoryWhenAuthorIsNull() throws IOException {
        FileData fileData = mock(FileData.class);
        when(fileData.getAuthor()).thenReturn(null);

        Repository repository = mock(S3Repository.class);
        when(repository.supports()).thenReturn(new FeaturesBuilder(repository).build());
        List<FileData> history = List.of(fileData);
        when(repository.listHistory(anyString())).thenReturn(history);

        PropertyResolver propertyResolver = mock(PropertyResolver.class);
        when(propertyResolver.getProperty("data.format.datetime")).thenReturn("mock.data.format");

        Comments comments = new Comments(propertyResolver, "id");

        HistoryRepositoryMapper historyRepositoryMapper = new HistoryRepositoryMapper(repository, comments);

        PageResponse<ProjectRevision> revisionHistory = historyRepositoryMapper.getProjectHistory("name", "filter", false, Pageable.unpaged());
        Assert.assertEquals(history.size(), revisionHistory.getContent().size());
    }

}
