package org.openl.studio.projects.model.history;

import java.time.Instant;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;

public class ProjectHistoryItem {

    @Parameter(description = "Local history entry identifier.")
    public final String id;

    @Parameter(description = """
            Moment the local version was kept at, in ISO 8601. Absent for the revision the module \
            was opened at, which no time names.""")
    public final @Nullable Instant modifiedOn;

    @Parameter(description = "Whether this entry is the module version currently being edited.")
    public final @Nullable Boolean current;

    public ProjectHistoryItem(String id, @Nullable Instant modifiedOn, boolean current) {
        this.current = current ? true : null; // null - is to reduce payload on ~27% - 16 bytes per item
        this.id = id;
        this.modifiedOn = modifiedOn;
    }
}
