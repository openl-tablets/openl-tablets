package org.openl.rules.workspace.lw;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.workspace.ProjectKey;

/**
 * The projects of a local workspace as they were at one moment.
 *
 * <p>Another request may open or close a project while a listing goes over the workspace. Every answer of this view
 * comes from the same moment, so such a project is in all of them or in none. A listing that matched it from one
 * moment and listed it from another would hold its folder twice.
 *
 * <p>The projects keep the order they are given in. When several projects of a repository show one name, the first
 * of them is the one found by that name.
 */
@NullMarked
public final class LocalProjects {

    private final Map<ProjectKey, AProject> byFolder;
    private final Map<ProjectKey, AProject> byName = new HashMap<>();

    /**
     * @param byFolder the projects by their repository and the folder they were taken from
     */
    public LocalProjects(Map<ProjectKey, AProject> byFolder) {
        this.byFolder = Collections.unmodifiableMap(new LinkedHashMap<>(byFolder));
        this.byFolder.values().forEach(project -> byName.putIfAbsent(
                ProjectKey.byName(project.getRepository().getId(), project.getBusinessName()), project));
    }

    /**
     * The project taken from the given folder of the repository.
     */
    public @Nullable AProject forFolder(String repositoryId, String folder) {
        return byFolder.get(new ProjectKey(repositoryId, folder));
    }

    /**
     * A project of the repository shown under the given name. The case of the name is ignored.
     */
    public @Nullable AProject forName(String repositoryId, String name) {
        return byName.get(ProjectKey.byName(repositoryId, name));
    }

    /**
     * All the projects.
     */
    public Collection<AProject> projects() {
        return byFolder.values();
    }
}
