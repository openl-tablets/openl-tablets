/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.web.repository.RepositoryTreeController;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.uw.UserWorkspaceProject;

import org.openl.util.benchmark.BenchmarkInfo;

import java.io.IOException;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;


/**
 * DOCUMENT ME!
 *
 * @author snshor
 */
public class WebStudio {
    private final static Log log = LogFactory.getLog(WebStudio.class);
    private final String workspacePath;
    ArrayList benchmarks = new ArrayList();
    List<StudioListener> listeners = new ArrayList<StudioListener>();
    int tableID = -1;
    ProjectModel model = new ProjectModel(this);
    final OpenLProjectLocator locator;
    OpenLWrapperInfo[] wrappers = null;
    WebStudioMode mode = WebStudioMode.BUSINESS1;
    private Set<String> writableProjects;
    OpenLWrapperInfo currentWrapper;
    WebStudioProperties properties = new WebStudioProperties();

    // public void toggleMode() throws Exception
    // {
    // mode = mode == WebStudioMode.BUSINESS ? WebStudioMode.DEVELOPER
    // : WebStudioMode.BUSINESS;
    // model.reset();
    // }
    int businessModeIdx = 0;
    int developerModeIdx = 0;
    WebStudioMode[] businessModes = {
            WebStudioMode.BUSINESS1, WebStudioMode.BUSINESS2, WebStudioMode.BUSINESS3
        };
    WebStudioMode[] developerModes = { WebStudioMode.DEVELOPER };

    public WebStudio() {
        this((System.getProperty("openl.webstudio.home") == null) ? ".."
            : System.getProperty("openl.webstudio.home"));
    }

    public WebStudio(String workspacePath) {
        this.workspacePath = workspacePath;
        locator = new OpenLProjectLocator(workspacePath);
    }

    public void reset() throws Exception {
        model.reset();
        for (StudioListener listener : listeners) {
            listener.studioReset();
        }
    }

    public void addBenchmark(BenchmarkInfo bi) {
        benchmarks.add(0, bi);
    }

    public void removeBenchmark(int i) {
        benchmarks.remove(i);
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the locator.
     */
    public OpenLProjectLocator getLocator() {
        return locator;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the model.
     */
    public ProjectModel getModel() {
        return model;
    }

    public void setWritableProjects(Set<String> writableProjects) {
        this.writableProjects = writableProjects;
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the wrappers.
     *
     * @throws IOException
     */
    public synchronized OpenLWrapperInfo[] getWrappers() throws IOException {
        if (wrappers == null) {
            wrappers = locator.listOpenLProjects();
        }
        return wrappers;
    }

    public void select(String name) throws Exception {
        OpenLWrapperInfo[] ww = getWrappers();
        if (name == null) {
            if (currentWrapper != null) {
                return;
            }

            if (ww.length > 0) {
                setCurrentWrapper(ww[0]);
            }
            return;
        }
        for (int i = 0; i < ww.length; i++) {
            if (ww[i].getWrapperClassName().equals(name)) {
                setCurrentWrapper(ww[i]);
                return;
            }
        }
        if (ww.length > 0) {
            setCurrentWrapper(ww[0]);
        }

        // throw new RuntimeException("Unknown wrapper: " + name);
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the currentWrapper.
     */
    public OpenLWrapperInfo getCurrentWrapper() {
        return currentWrapper;
    }

    /**
     * DOCUMENT ME!
     *
     * @param wrapper The currentWrapper to set.
     *
     * @throws Exception
     */
    public void setCurrentWrapper(OpenLWrapperInfo wrapper) throws Exception {
        if (this.currentWrapper != wrapper) {
            model.setWrapperInfo(wrapper);
            model.setReadOnly(!((writableProjects == null)
                    || writableProjects.contains(wrapper.getProjectInfo().getName())));
        }
        this.currentWrapper = wrapper;
        for (StudioListener listener : listeners) {
            listener.studioReset();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return Returns the tableID.
     */
    public int getTableID() {
        return tableID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param tableID The tableID to set.
     */
    public void setTableID(int tableID) {
        this.tableID = tableID;
    }

    public WebStudioProperties getProperties() {
        return this.properties;
    }

    public void setProperties(WebStudioProperties properties) {
        this.properties = properties;
    }

    public WebStudioMode getMode() {
        return this.mode;
    }

    public void setMode(WebStudioMode mode) {
        this.mode = mode;
    }

    public void setMode(String modeStr) throws Exception {
        boolean sameType = modeStr.equals(mode.getType());

        if ("business".equals(modeStr)) {
            businessModeIdx = findMode(businessModes, businessModeIdx, sameType);
        } else if ("developer".equals(modeStr)) {
            developerModeIdx = findMode(developerModes, developerModeIdx, sameType);
        } else {
            throw new RuntimeException("Invalid Mode: " + modeStr);
        }

        model.redraw();
    }

    /**
     * DOCUMENT ME!
     *
     * @param modes
     * @param modeIdx
     * @param sameType
     *
     * @return
     */
    private int findMode(WebStudioMode[] modes, int modeIdx, boolean sameType) {
        if (sameType) {
            modeIdx = (modeIdx + 1) % modes.length;
        }

        mode = modes[modeIdx];
        return modeIdx;
    }

    public void addEventListener(StudioListener listener) {
        listeners.add(listener);
    }

    public boolean removeListener(StudioListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Returns path on local file system to openL workspace this instance of web
     * studio works with.
     *
     * @return path to openL projects workspace, i.e. folder containing openL projects.
     */
    public String getWorkspacePath() {
        return workspacePath;
    }

    public BenchmarkInfo[] getBenchmarks() {
        return (BenchmarkInfo[]) this.benchmarks.toArray(new BenchmarkInfo[0]);
    }

    public UserWorkspaceProject getCurrentProject(HttpSession session)
        throws ProjectException, WorkspaceException
    {
        if (currentWrapper==null) {
            return null;
        }
        String projectName = currentWrapper.getProjectInfo().getName();
        RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
        UserWorkspaceProject project = rulesUserSession.getUserWorkspace()
                .getProject(projectName);
        return project;
    }

    public void executeOperation(String operation, HttpSession session) {
        if ("checkIn".equals(operation)) {
            try {
                UserWorkspaceProject project = getCurrentProject(session);
                if (project==null) {
                    return;
                }
                project.checkIn();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.error("Can not check in", e);
                }
            }
        }
        if ("checkOut".equals(operation)) {
            try {
                UserWorkspaceProject project = getCurrentProject(session);
                if (project==null) {
                    return;
                }
                project.checkOut();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.error("Can not check out", e);
                }
            }
        }
    }

    static interface StudioListener extends EventListener {
        void studioReset();
    }
}
