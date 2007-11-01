package org.openl.rules.uw;

import org.openl.rules.commons.projects.Project;
import org.openl.rules.commons.projects.ProjectException;
import org.openl.rules.commons.projects.ProjectVersion;

import java.util.Collection;

public interface UserWorkspaceProject extends Project {
    void close() throws ProjectException;
    void open() throws ProjectException;
    void openVersion(ProjectVersion version) throws ProjectException;
    void checkOut() throws ProjectException;
    void checkIn() throws ProjectException;

    Collection<ProjectVersion> getVersions();

    // is checked-out by me? -- in LW + locked by me
    boolean isCheckedOut();
    // is opened by me? -- in LW
    boolean isOpened();
    // is deleted in DTR
    boolean isDeleted();
    // is locked in DTR
    boolean isLocked();
}
