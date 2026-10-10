package org.openl.rules.project.impl.local;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * The files of a workspace project changed locally since the project was opened or saved.
 *
 * <p>Each path is relative to the project folder and starts with {@code /}, as the file baselines name them. A file
 * changed outside OpenL Studio in the repository the project came from is no local change.
 *
 * @param added    the files that have no baseline
 * @param modified the files that differ from their baseline
 * @param deleted  the baselines that have no file
 * @author Yury Molchan
 */
@NullMarked
public record LocalChanges(List<String> added, List<String> modified, List<String> deleted) {

    /**
     * Returns whether no file is changed.
     */
    public boolean isEmpty() {
        return added.isEmpty() && modified.isEmpty() && deleted.isEmpty();
    }
}
