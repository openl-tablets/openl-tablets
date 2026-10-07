package org.openl.studio.projects.service;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.RulesProject;

/**
 * Caches project tags per repository revision.
 *
 * <p>Tags are a committed project property, so the tags of a given revision are immutable and identical for
 * every user. They are cached by repository, project and revision: a new revision — any commit, including a
 * tag edit — is a new key and reads fresh.
 *
 * <p>Opened projects are read fresh: their working copy may hold uncommitted tag edits that must not leak
 * between users through a shared cache. Reading the tags file is a repository read, so caching turns the
 * per-request scan of every project's tags into a single read per revision, shared across users.
 */
@Component
@RequiredArgsConstructor
public class ProjectTagsCache {

    static final String CACHE_NAME = "projectTags";

    private final CacheManager cacheManager;

    public Map<String, String> getTags(RulesProject project) {
        if (project.isOpened()) {
            return project.getLocalTags();
        }
        var revision = revisionOf(project);
        var cache = revision == null ? null : cacheManager.getCache(CACHE_NAME);
        if (cache == null) {
            return project.getLocalTags();
        }
        return cache.get(key(project, revision), project::getLocalTags);
    }

    private static String revisionOf(RulesProject project) {
        try {
            var fileData = project.getFileData();
            return fileData == null ? null : fileData.getVersion();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String key(RulesProject project, String revision) {
        return project.getDesignRepository().getId() + '/' + project.getName() + '@' + revision;
    }
}
