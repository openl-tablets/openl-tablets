package org.openl.rules.webstudio.web.repository.merge;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;

public class ConflictResolution {
    private ResolutionType resolutionType = ResolutionType.UNRESOLVED;
    private ProjectFile customResolutionFile;

    public ResolutionType getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(ResolutionType resolutionType) {
        this.resolutionType = resolutionType == null ? ResolutionType.UNRESOLVED : resolutionType;
    }

    public ProjectFile getCustomResolutionFile() {
        return customResolutionFile;
    }

    public void setCustomResolutionFile(ProjectFile customResolutionFile) {
        if (this.customResolutionFile != null) {
            this.customResolutionFile.destroy();
        }
        this.customResolutionFile = customResolutionFile;
    }

    public void destroy() {
        if (customResolutionFile != null) {
            customResolutionFile.destroy();
            customResolutionFile = null;
        }
    }
}
