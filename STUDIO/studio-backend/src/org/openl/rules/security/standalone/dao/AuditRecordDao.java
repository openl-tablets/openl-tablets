package org.openl.rules.security.standalone.dao;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.openl.rules.security.standalone.persistence.AuditRecord;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditQuery;

/**
 * Reads and cleans the audit log. Events are listed newest first.
 */
public interface AuditRecordDao extends Dao<AuditRecord> {

    /**
     * Reads a page of the events a query matches.
     *
     * @param query  which events to read
     * @param offset how many matching events to skip
     * @param limit  how many events to read at most
     * @return the events, newest first
     */
    List<AuditRecord> find(AuditQuery query, int offset, int limit);

    /**
     * Reads the events a query matches that were recorded before a given event. Reading on from the last event of
     * each batch walks the whole log, unaffected by the events recorded meanwhile.
     *
     * <p>The events read are not kept by the session, so a walk over a large log holds one batch at a time.
     *
     * @param query    which events to read
     * @param beforeId the id of the event to read on from, or {@code null} to start with the newest one
     * @param limit    how many events to read at most
     * @return the events, newest first
     */
    List<AuditRecord> findBefore(AuditQuery query, @Nullable Long beforeId, int limit);

    /**
     * Counts the events a query matches, up to a limit: the count stops once it reaches the limit, so a query that
     * matches a large part of the log is never counted whole.
     */
    long count(AuditQuery query, int limit);

    /**
     * Reads the action of the last event of an actor.
     *
     * @param actor the name of the actor, as the log keeps it
     * @return the action, or {@code null} when the log holds no event of the actor
     */
    @Nullable
    AuditAction lastActionOf(String actor);

    /**
     * Removes the oldest events recorded before a moment, at most the given number of them.
     *
     * @return how many events were removed
     */
    int deleteBefore(Instant time, int limit);
}
