package org.openl.studio.projects.messaging;

import static org.mockito.Mockito.verify;

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
    void broadcasts_when_a_repository_changes() {
        bridge.onRepositoryModified();

        verify(broadcaster).broadcastChanged();
    }
}
