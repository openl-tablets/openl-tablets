package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.openl.rules.workspace.dtr.DesignTimeRepository;

@ExtendWith(MockitoExtension.class)
class DesignRepositoryWebSocketBridgeTest {

    @Mock
    private DesignTimeRepository designTimeRepository;

    @Mock
    private ProjectsChangedBroadcaster broadcaster;

    @InjectMocks
    private DesignRepositoryWebSocketBridge bridge;

    @Test
    void listens_to_the_design_repositories_for_its_whole_life() {
        bridge.register();
        verify(designTimeRepository).addListener(bridge);

        bridge.unregister();
        verify(designTimeRepository).removeListener(bridge);
    }

    @Test
    void broadcasts_when_a_repository_holds_something_new() {
        bridge.onRepositoryContentChanged();

        verify(broadcaster).broadcastChanged();
    }

    @Test
    void says_nothing_when_a_re_read_found_the_repository_as_it_was() {
        // A re-read also runs for work that leaves the repository alone — opening a project
        // re-indexes and finds the same trees — and every session answers a ping by re-reading its
        // whole workspace.
        bridge.onRepositoryModified();

        verifyNoInteractions(broadcaster);
    }
}
