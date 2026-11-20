package org.openl.studio.projects.model.project.status;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;

import org.openl.rules.workspace.ProjectId;

@Builder(toBuilder = true)
public record ProjectStatusViewModel (
        @Parameter(description = "Project identifier.")
        ProjectId projectId,

        @Parameter(description = "Branch name the project is currently opened on. Applicable only to repositories that support branches.")
        String branch,

        @Parameter(description = "Revision of the project currently opened.")
        String revision,

        @Parameter(description = "Current compilation state of the project.")
        CompileState compileState,

        @Parameter(description = "Information about the user who last modified the project.")
        ModifiedBy lastModifiedBy,

        @Parameter(description = """
                Compilation details: messages and module progress. An opened project nobody compiled yet tells \
                only the messages of the check of its localization files""")
        CompilationDetails compilation,

        @Parameter(description = """
                Set when a workbook the module the session has open is compiled with was written to while \
                automatic compilation is switched off, so it is the reader who asks for it to be compiled. Absent \
                where nothing is waiting""")
        Boolean manualCompileNeeded,

        @Parameter(description = """
                Set when another program, such as Excel or an IDE, changed a file the module the session has open is \
                compiled from since the compilation read it. What the session shows of the file is what it held \
                before; compiling the module with reset=true reads it again. Absent where nothing changed""")
        Boolean changedOnDisk,

        @Parameter(description = "Files modified locally and not yet committed to the design repository.")
        PendingChanges pendingChanges,

        @Parameter(description = """
                Orders the statuses the server tells: a status read later has a larger sequence, whether it is read \
                over REST or pushed over the WebSocket. A client keeps the status with the largest sequence. The \
                receipt of a subscription to a status destination carries the sequence the subscription started at \
                in its status-sequence header: a status with a smaller one may have changed unheard, and is read \
                again""")
        long sequence
) {
}
