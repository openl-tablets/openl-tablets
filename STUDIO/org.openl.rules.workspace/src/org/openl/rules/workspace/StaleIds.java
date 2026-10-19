package org.openl.rules.workspace;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The ids that stopped naming something: the id each leads to now, or that what it named was deleted.
 *
 * <p>An id leads straight to the id at the end of its chain of changes. An id of something deleted stays deleted
 * until it is taken again, and an id that is taken again names no change any more.
 *
 * <p>The most recent changes are kept: past the capacity, the eldest are forgotten.
 *
 * @param <K> the type of the ids
 * @author Yury Molchan
 */
public final class StaleIds<K> {

    /** What became of what an id named. */
    public sealed interface Change<K> permits Moved, Deleted {
    }

    /**
     * It is known by another id now.
     *
     * @param current the id it has now
     */
    public record Moved<K>(K current) implements Change<K> {
    }

    /** It was deleted. */
    public record Deleted<K>() implements Change<K> {
    }

    private final Map<K, Change<K>> changes;

    /**
     * @param capacity how many changes are kept
     */
    public StaleIds(int capacity) {
        changes = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, Change<K>> eldest) {
                return size() > capacity;
            }
        };
    }

    /**
     * What had the old id until now is known by the new one: the ids that led to the old one lead to the new one too.
     *
     * <p>Learned also for an id known as deleted: something took that id again before it changed.
     */
    public synchronized void moved(K from, K to) {
        if (!from.equals(to)) {
            changed(from, new Moved<>(to));
            // The new id names something now, which no change of before leads elsewhere.
            changes.remove(to);
        }
    }

    /**
     * What had the old id is known by the new one, unless the old id is known as deleted: a change found by comparing
     * what was before with what is now gives nothing deleted a new id, though something else later stood where it
     * stood.
     */
    public synchronized void movedUnlessDeleted(K from, K to) {
        if (!(changes.get(from) instanceof Deleted<K>)) {
            moved(from, to);
        }
    }

    /** What had the id until now was deleted: the ids that led to it tell so too. */
    public synchronized void deleted(K id) {
        changed(id, new Deleted<>());
    }

    /** The id names something now, so it names no change. */
    public synchronized void taken(K id) {
        changes.remove(id);
    }

    /**
     * What became of what the id named.
     *
     * @return the change, or nothing when no change of the id is known
     */
    public synchronized Optional<Change<K>> changeOf(K id) {
        return Optional.ofNullable(changes.get(id));
    }

    private void changed(K id, Change<K> change) {
        var leadingHere = new Moved<>(id);
        changes.replaceAll((stale, current) -> current.equals(leadingHere) ? change : current);
        // The most recent change is the last one forgotten.
        changes.remove(id);
        changes.put(id, change);
    }
}
