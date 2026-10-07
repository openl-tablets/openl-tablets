package org.openl.studio.projects.messaging;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.DesignTimeRepositoryListener;

/**
 * Pings every user on {@code /user/topic/projects/changed} whenever the content of a design
 * repository changes — a commit or merge from any user, or an external push the repository's polling
 * detects. A re-read that finds the repository as it was says nothing: it happens for work that
 * changes nothing in it, and every session answers a ping by re-reading its whole workspace.
 *
 * The design-repository listener carries no detail (which repository, which project), so the ping does
 * not either: every subscriber re-reads the projects list through the REST API under its own ACL.
 * A repository operation touching many files collapses into one ping.
 */
@Component
@RequiredArgsConstructor
public class DesignRepositoryWebSocketBridge implements DesignTimeRepositoryListener {

    private final DesignTimeRepository designTimeRepository;
    private final ProjectsChangedBroadcaster broadcaster;

    @PostConstruct
    void register() {
        designTimeRepository.addListener(this);
    }

    @PreDestroy
    void unregister() {
        designTimeRepository.removeListener(this);
    }

    @Override
    public void onRepositoryModified() {
        // A re-read on its own says nothing to the sessions: it also runs for work that leaves the
        // repository as it was, and every session answers a ping by re-reading its whole workspace.
    }

    @Override
    public void onRepositoryContentChanged() {
        broadcaster.broadcastChanged();
    }
}
