package org.openl.rules.security.standalone.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.openl.rules.security.standalone.persistence.AuditRecord;
import org.openl.studio.audit.model.AuditAction;
import org.openl.studio.audit.model.AuditQuery;

/**
 * Hibernate implementation of {@link AuditRecordDao}.
 */
@Component("auditRecordDao")
public class AuditRecordDaoImpl extends BaseHibernateDao<AuditRecord> implements AuditRecordDao {

    private static final String ID = "id";
    private static final String EVENT_TIME = "eventTime";

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecord> find(AuditQuery query, int offset, int limit) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var criteria = cb.createQuery(AuditRecord.class);
        var root = criteria.from(AuditRecord.class);
        criteria.select(root)
                .where(matching(cb, root, query).toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get(ID)));
        return session.createQuery(criteria)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecord> findBefore(AuditQuery query, @Nullable Long beforeId, int limit) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var criteria = cb.createQuery(AuditRecord.class);
        var root = criteria.from(AuditRecord.class);
        var predicates = matching(cb, root, query);
        if (beforeId != null) {
            predicates.add(cb.lt(root.get(ID), beforeId));
        }
        criteria.select(root)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.get(ID)));
        var events = session.createQuery(criteria)
                .setMaxResults(limit)
                .getResultList();
        events.forEach(session::detach);
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public long count(AuditQuery query, int limit) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var criteria = cb.createQuery(Long.class);
        var root = criteria.from(AuditRecord.class);
        criteria.select(root.get(ID)).where(matching(cb, root, query).toArray(new Predicate[0]));
        return session.createQuery(criteria).setMaxResults(limit).getResultList().size();
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable AuditAction lastActionOf(String actor) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        var criteria = cb.createQuery(AuditAction.class);
        var root = criteria.from(AuditRecord.class);
        criteria.select(root.get("action"))
                .where(cb.equal(root.get("actor"), actor))
                .orderBy(cb.desc(root.get(ID)));
        return session.createQuery(criteria).setMaxResults(1).getResultList().stream().findFirst().orElse(null);
    }

    @Override
    public int deleteBefore(Instant time, int limit) {
        var session = getSession();
        var cb = session.getCriteriaBuilder();
        // The id of the last event the batch takes: the ids grow as the events are recorded. A batch is bounded by
        // it, since a removal cannot be limited the same way on every database.
        var last = cb.createQuery(Long.class);
        var events = last.from(AuditRecord.class);
        last.select(events.get(ID))
                .where(cb.lessThan(events.get(EVENT_TIME), time))
                .orderBy(cb.asc(events.get(ID)));
        var lastId = session.createQuery(last).setFirstResult(limit - 1).setMaxResults(1).getResultList();

        var delete = cb.createCriteriaDelete(AuditRecord.class);
        var root = delete.from(AuditRecord.class);
        var older = cb.lessThan(root.get(EVENT_TIME), time);
        delete.where(lastId.isEmpty() ? older : cb.and(older, cb.le(root.get(ID), lastId.getFirst())));
        return session.createMutationQuery(delete).executeUpdate();
    }

    private static List<Predicate> matching(CriteriaBuilder cb, Root<AuditRecord> root, AuditQuery query) {
        var predicates = new ArrayList<Predicate>();
        if (query.from() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(EVENT_TIME), query.from()));
        }
        if (query.to() != null) {
            predicates.add(cb.lessThan(root.get(EVENT_TIME), query.to()));
        }
        var actions = actionsOf(query);
        if (actions != null) {
            predicates.add(actions.isEmpty() ? cb.disjunction() : root.get("action").in(actions));
        }
        if (query.outcome() != null) {
            predicates.add(cb.equal(root.get("outcome"), query.outcome()));
        }
        if (query.authentication() != null) {
            predicates.add(cb.equal(root.get("authentication"), query.authentication()));
        }
        addContains(cb, root, "actor", query.actor(), predicates);
        addContains(cb, root, "target", query.target(), predicates);
        addContains(cb, root, "subject", query.subject(), predicates);
        addContains(cb, root, "clientAddress", query.clientAddress(), predicates);
        return predicates;
    }

    /**
     * The actions the events may have: the actions the query names that belong to the categories it names. A category
     * is searched by its actions, so one index of the actions serves both criteria.
     *
     * @return the actions, or {@code null} when the query names neither actions nor categories
     */
    private static @Nullable Set<AuditAction> actionsOf(AuditQuery query) {
        if (query.categories().isEmpty()) {
            return query.actions().isEmpty() ? null : query.actions();
        }
        return Arrays.stream(AuditAction.values())
                .filter(action -> query.categories().contains(action.getCategory()))
                .filter(action -> query.actions().isEmpty() || query.actions().contains(action))
                .collect(Collectors.toSet());
    }

    private static void addContains(CriteriaBuilder cb,
                                    Root<AuditRecord> root,
                                    String field,
                                    @Nullable String text,
                                    List<Predicate> predicates) {
        if (text != null && !text.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get(field)), "%" + escape(text.trim()) + "%", ESCAPE_CHAR));
        }
    }
}
