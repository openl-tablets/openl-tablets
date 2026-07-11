package org.openl.studio.projects.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;

class ProjectTagsCacheTest {

    private final ProjectTagsCache cache =
            new ProjectTagsCache(new ConcurrentMapCacheManager(ProjectTagsCache.CACHE_NAME));

    private static RulesProject project(boolean opened, String revision, Map<String, String> tags) {
        var project = mock(RulesProject.class);
        when(project.isOpened()).thenReturn(opened);
        when(project.getName()).thenReturn("Proj");
        var repository = mock(Repository.class);
        when(repository.getId()).thenReturn("design");
        when(project.getDesignRepository()).thenReturn(repository);
        when(project.getFileData()).thenReturn(fileDataAt(revision));
        when(project.getLocalTags()).thenReturn(tags);
        return project;
    }

    private static FileData fileDataAt(String revision) {
        var fileData = new FileData();
        fileData.setVersion(revision);
        return fileData;
    }

    @Test
    void reads_committed_tags_once_and_serves_the_rest_from_cache() {
        var project = project(false, "v1", Map.of("Category", "Payroll"));

        var first = cache.getTags(project);
        var second = cache.getTags(project);

        assertEquals(Map.of("Category", "Payroll"), first);
        assertEquals(first, second);
        verify(project, times(1)).getLocalTags();
    }

    @Test
    void re_reads_when_the_revision_changes() {
        var project = project(false, "v1", Map.of("Category", "Payroll"));
        cache.getTags(project);

        when(project.getFileData()).thenReturn(fileDataAt("v2"));
        cache.getTags(project);

        verify(project, times(2)).getLocalTags();
    }

    @Test
    void reads_opened_projects_fresh_so_uncommitted_edits_never_leak() {
        var project = project(true, "v1", Map.of("Category", "Payroll"));

        cache.getTags(project);
        cache.getTags(project);

        verify(project, times(2)).getLocalTags();
    }
}
