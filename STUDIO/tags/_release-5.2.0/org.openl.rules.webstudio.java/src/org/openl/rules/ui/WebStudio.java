/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.uw.UserWorkspaceProject;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.benchmark.BenchmarkInfo;

import java.io.IOException;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpSession;
import javax.faces.context.FacesContext;

/**
 * DOCUMENT ME!
 *
 * @author snshor
 */
public class WebStudio {
    private final static Log log = LogFactory.getLog(WebStudio.class);

    private String workspacePath;
    ArrayList<BenchmarkInfo> benchmarks = new ArrayList<BenchmarkInfo>();
    List<StudioListener> listeners = new ArrayList<StudioListener>();
    String tableUri;
    ProjectModel model = new ProjectModel(this);
    private OpenLProjectLocator locator;
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
        boolean initialized = false;
        try {
            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
            initialized = init(session);
        } catch (Exception e) {}

        if (!initialized) {
            workspacePath = System.getProperty("openl.webstudio.home") == null ? ".." : System.getProperty("openl.webstudio.home");
            locator = new OpenLProjectLocator(workspacePath);
        }
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

    public String getTableUri() {
        return tableUri;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
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
        return benchmarks.toArray(new BenchmarkInfo[0]);
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
                log.error("Can not check in!", e);
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
                log.error("Can not check out!", e);
            }
        }
    }

    public boolean init(HttpSession session) {
        UserWorkspace userWorkspace;
        try {
            RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
            userWorkspace = rulesUserSession.getUserWorkspace();
        } catch (WorkspaceException e) {
            log.error("Failed to get user workspace", e);
            return false;
        } catch (ProjectException e) {
            log.error("Failed to get user workspace", e);
            return false;
        }
        if (userWorkspace == null) {
            return false;
        }

        workspacePath = userWorkspace.getLocalWorkspaceLocation().getAbsolutePath();
        Set<String> writableProjects = new HashSet<String>();
        for (Project project : userWorkspace.getProjects()) {
            UserWorkspaceProject workspaceProject = (UserWorkspaceProject) project;
            if (workspaceProject.isCheckedOut() || workspaceProject.isLocalOnly()) {
                writableProjects.add(workspaceProject.getName());
            }
        }
        setWritableProjects(writableProjects);
        locator = new OpenLProjectLocator(workspacePath);
        return true;
    }

    static interface StudioListener extends EventListener {
        void studioReset();
    }
}
