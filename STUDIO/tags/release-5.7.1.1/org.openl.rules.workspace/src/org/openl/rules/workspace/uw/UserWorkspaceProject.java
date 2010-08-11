package org.openl.rules.workspace.uw;

import java.io.File;
import java.util.Collection;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;

public interface UserWorkspaceProject extends Project, UserWorkspaceProjectFolder {
    void checkIn() throws ProjectException;

    void checkIn(int major, int minor) throws ProjectException;

    void checkOut() throws ProjectException;

    void close() throws ProjectException;

    void erase() throws ProjectException;

    /**
     * Exports project version into zip file.
     *
     * @param version version of project to be exported
     * @return zip file with project
     * @throws ProjectException if failed
     */
    File exportVersion(CommonVersion version) throws ProjectException;

    LockInfo getLockInfo();

    Collection<ProjectVersion> getVersions();

    /** is checked-out by me? -- in LW + locked by me */
    boolean isCheckedOut();

    /** is deleted in DTR */
    boolean isDeleted();

    boolean isDeploymentProject();

    /** no such project in DTR */
    boolean isLocalOnly();

    /** is locked in DTR */
    boolean isLocked();

    /** is opened by me? -- in LW */
    boolean isOpened();

    /** is opened other version? (not last) */
    boolean isOpenedOtherVersion();

    boolean isRulesProject();

    void open() throws ProjectException;

    void openVersion(CommonVersion version) throws ProjectException;

    void undelete() throws ProjectException;
}
