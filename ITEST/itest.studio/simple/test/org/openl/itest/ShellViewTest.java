package org.openl.itest;

import org.junit.jupiter.api.Test;

import org.openl.itest.core.JettyServer;

/**
 * The rules editor is one shell page that keeps loading its panels and its content as pages of their own.
 * The shell stays open for the whole session while those pages come and go, so it must go on answering its
 * own requests however many of them have been loaded since.
 *
 * <p>The server keeps only a bounded number of pages per session, and a shell counted among them is dropped
 * once that many panels have been loaded. The page then reports every request of its own as expired, and the
 * editor answers each report by loading its content again — which asks again, and never stops.
 */
class ShellViewTest {

    /** More pages than the server keeps per session, so a shell that is kept among them is dropped. */
    private static final int PANEL_LOADS = 20;

    @Test
    void shellAnswersAfterManyPanelLoads() throws Exception {
        try (var client = JettyServer.get().start()) {
            client.send("shell-view/010-open-shell");
            for (var i = 0; i < PANEL_LOADS; i++) {
                client.send("shell-view/020-load-panel");
            }
            client.send("shell-view/030-page-unload");
        }
    }
}
