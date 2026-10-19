package org.openl.rules.workspace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Remembers the ids that stopped naming a project of a design repository, so that a client holding such an id is
 * served the project it names now, or told that the project was deleted.
 *
 * <p>The ledger learns from the listings of each repository that were read whole: a folder the listing before showed
 * a project in and this one does not lost its project. When exactly one folder lost a project of some name and
 * exactly one new folder shows a project of that name, the project moved there. Any other lost project was deleted.
 * The first listing of a repository has nothing to be compared with, so it teaches nothing.
 *
 * <p>Each repository keeps its most recent changes in memory. They are lost when the server restarts: an id from
 * before is unknown then. An id a project has now never names a change: the listing that shows it takes it out of
 * the ledger. A chain of moves leads to the id at its end, and an id of a deleted project stays deleted until a
 * project takes it again.
 *
 * @author Yury Molchan
 */
public final class StaleProjectIds {

    /** How many changes each repository remembers. */
    private static final int CHANGES = 1000;

    private final Map<String, Ledger> ledgers = new HashMap<>();

    /**
     * Learns the changes a listing of a repository shows, compared with the listing before.
     *
     * @param repositoryId the repository the listing was read from
     * @param names        the name of each project the listing shows, by its id: every project of the repository
     */
    public synchronized void listed(String repositoryId, Map<ProjectId, String> names) {
        var ledger = ledgers.computeIfAbsent(repositoryId, id -> new Ledger());
        var before = ledger.listing;
        ledger.listing = Map.copyOf(names);
        if (before == null) {
            return;
        }
        var found = absentFrom(before, names);
        absentFrom(names, before).forEach((name, lost) -> {
            var to = found.getOrDefault(name, List.of());
            if (lost.size() == 1 && to.size() == 1) {
                ledger.changes.moved(lost.getFirst(), to.getFirst());
            } else {
                lost.forEach(ledger.changes::deleted);
            }
        });
        found.values().forEach(ids -> ids.forEach(ledger.changes::taken));
    }

    /**
     * What became of the project an id named, for an id no project has.
     *
     * @param id the id a request names a project by
     * @return the change, or nothing when the ledger knows nothing of the id
     */
    public synchronized Optional<StaleIds.Change<ProjectId>> changeOf(ProjectId id) {
        return ledgers.values().stream()
                .flatMap(ledger -> ledger.changes.changeOf(id).stream())
                .findFirst();
    }

    /**
     * The projects one listing shows and the other does not, by their names regardless of case.
     */
    private static Map<String, List<ProjectId>> absentFrom(Map<ProjectId, String> other,
                                                           Map<ProjectId, String> listing) {
        var absent = new HashMap<String, List<ProjectId>>();
        listing.forEach((id, name) -> {
            if (!other.containsKey(id)) {
                absent.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> new ArrayList<>()).add(id);
            }
        });
        return absent;
    }

    /** The changes of the projects of one repository, and the listing they were learned from. */
    private static final class Ledger {

        private @Nullable Map<ProjectId, String> listing;

        private final StaleIds<ProjectId> changes = new StaleIds<>(CHANGES);
    }
}
