package org.openl.rules.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;

class ProjectCompilationStatusTest {

    @Test
    void readsTheTraceBehindTheMessageItIsAskedAbout() {
        var broken = new OpenLErrorMessage(new OpenLRuntimeException("Identifier is not found"));
        var status = ProjectCompilationStatus.newBuilder()
                .addMessages(List.of(new OpenLMessage("Deprecated", Severity.WARN), broken))
                .build();

        var trace = status.getStacktrace(broken.getId());

        assertTrue(trace != null && trace.contains("Identifier is not found"));
    }

    @Test
    void answersNothingForAMessageThatCarriesNoTrace() {
        var warning = new OpenLMessage("Deprecated", Severity.WARN);
        var status = ProjectCompilationStatus.newBuilder().addMessages(List.of(warning)).build();

        // A warning was raised about nothing that failed, and a message nobody raised is not there to read.
        assertNull(status.getStacktrace(warning.getId()));
        assertNull(status.getStacktrace(-1));
    }
}
