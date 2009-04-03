package org.openl.rules.workspace.uw;

import java.io.File;
import java.util.Collection;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.LockInfo;

public interface UserWorkspaceProject extends Project, UserWorkspaceProjectFolder {
    void close() throws ProjectException;
    void open() throws ProjectException;
    void openVersion(CommonVersion version) throws ProjectException;
    void checkOut() throws ProjectException;
    void checkIn() throws ProjectException;
    void checkIn(int major, int minor) throws ProjectException;
    /**
     * Exports project version into zip file.
     * 
     * @param version version of project to be exported
     * @return zip file with project
     * @throws ProjectException if failed
     */
    File exportVersion(CommonVersion version) throws ProjectException;

    Collection<ProjectVersion> getVersions();

    /** is checked-out by me? -- in LW + locked by me */
    boolean isCheckedOut();
    /** is opened by me? -- in LW */
    boolean isOpened();
    /** is opened other version? (not last) */
    boolean isOpenedOtherVersion();
    /** is deleted in DTR */
    boolean isDeleted();
    /** is locked in DTR */
    boolean isLocked();
    /** no such project in DTR */
    boolean isLocalOnly();

    boolean isRulesProject();
    boolean isDeploymentProject();

    void undelete() throws ProjectException;
    void erase() throws ProjectException;

    LockInfo getLockInfo();
}
