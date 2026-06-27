package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import jakarta.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

/**
 * Session-scoped store of {@link ProjectModel}s, one per opened project/branch (see {@link ProjectModelKey}).
 * Lets a single user session compile and edit several projects in parallel — multiple browser tabs or async
 * REST edits to different projects — without one project's compilation clobbering another's.
 *
 * <p>Models are created lazily on first access and reused afterwards. The number of live models is bounded:
 * when the limit is exceeded the least-recently-used model that is idle (no compilation in flight) and not
 * pinned is {@link ProjectModel#destroy() destroyed} to release its dependency manager and worker threads.
 * The pinned key (the session's current selection) is never evicted.
 *
 * <p>All operations are synchronized; model creation and compilation happen under each model's own monitor, so
 * different projects still compile concurrently.
 */
@Slf4j
class ProjectModelRegistry {

    private final Function<ProjectModelKey, ProjectModel> factory;
    private final int maxModels;
    private final Predicate<ProjectModel> evictable;

    // Access-ordered so the eldest entry is the least recently used.
    private final LinkedHashMap<ProjectModelKey, ProjectModel> models = new LinkedHashMap<>(16, 0.75f, true);

    @Nullable
    private ProjectModelKey pinnedKey;

    ProjectModelRegistry(Function<ProjectModelKey, ProjectModel> factory,
                         int maxModels,
                         Predicate<ProjectModel> evictable) {
        this.factory = factory;
        this.maxModels = Math.max(1, maxModels);
        this.evictable = evictable;
    }

    synchronized ProjectModel getOrCreate(ProjectModelKey key) {
        ProjectModel model = models.get(key);
        if (model == null) {
            model = factory.apply(key);
            models.put(key, model);
            evictIfNeeded();
        }
        return model;
    }

    @Nullable
    synchronized ProjectModel get(ProjectModelKey key) {
        return models.get(key);
    }

    /**
     * The first live model matching the predicate, or {@code null}. Used to look up a model by its project
     * identity (rather than by key) for targeted invalidation/eviction.
     */
    @Nullable
    synchronized ProjectModel findFirst(Predicate<ProjectModel> predicate) {
        for (ProjectModel model : models.values()) {
            if (predicate.test(model)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Mark the key whose model must never be evicted (the session's current selection).
     */
    synchronized void setPinned(@Nullable ProjectModelKey key) {
        this.pinnedKey = key;
    }

    synchronized void remove(ProjectModelKey key) {
        destroy(models.remove(key));
    }

    /**
     * Remove and destroy every model matching the predicate, except the pinned one. Used to evict models whose
     * project is no longer opened.
     */
    synchronized void removeMatching(Predicate<ProjectModel> predicate) {
        Iterator<Map.Entry<ProjectModelKey, ProjectModel>> it = models.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (!entry.getKey().equals(pinnedKey) && predicate.test(entry.getValue())) {
                destroy(entry.getValue());
                it.remove();
            }
        }
    }

    synchronized void clear() {
        models.values().forEach(this::destroy);
        models.clear();
    }

    /**
     * Apply an action to every live model. Iterates a snapshot so the action may mutate the registry.
     */
    synchronized void forEach(Consumer<ProjectModel> action) {
        new ArrayList<>(models.values()).forEach(action);
    }

    synchronized int size() {
        return models.size();
    }

    private void evictIfNeeded() {
        Iterator<Map.Entry<ProjectModelKey, ProjectModel>> it = models.entrySet().iterator();
        while (models.size() > maxModels && it.hasNext()) {
            var entry = it.next();
            if (!entry.getKey().equals(pinnedKey) && evictable.test(entry.getValue())) {
                destroy(entry.getValue());
                it.remove();
            }
        }
    }

    private void destroy(@Nullable ProjectModel model) {
        if (model == null) {
            return;
        }
        try {
            model.destroy();
        } catch (RuntimeException e) {
            log.warn("Failed to destroy project model", e);
        }
    }
}
