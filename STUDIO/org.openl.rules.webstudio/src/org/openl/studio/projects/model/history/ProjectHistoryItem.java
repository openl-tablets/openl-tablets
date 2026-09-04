package org.openl.studio.projects.model.history;

import io.swagger.v3.oas.annotations.Parameter;
import org.jspecify.annotations.Nullable;

public class ProjectHistoryItem {

    @Parameter(description = "Local history entry identifier.")
    public final String id;

    @Parameter(description = "Date and time when the local version was created, or the revision baseline label.")
    public final String modifiedOn;

    @Parameter(description = "Whether this entry is the module version currently being edited.")
    public final @Nullable Boolean current;

    public ProjectHistoryItem(String id, String modifiedOn, boolean current) {
        this.current = current ? true : null; // null - is to reduce payload on ~27% - 16 bytes per item
        this.id = id;
        this.modifiedOn = modifiedOn;
    }
}
