/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.view.BaseBusinessViewMode;
import org.openl.rules.ui.view.BaseDeveloperViewMode;
import org.openl.rules.ui.view.BusinessViewMode1;
import org.openl.rules.ui.view.BusinessViewMode2;
import org.openl.rules.ui.view.BusinessViewMode3;
import org.openl.rules.ui.view.DeveloperByFileViewMode;
import org.openl.rules.ui.view.DeveloperByTypeViewMode;
import org.openl.rules.ui.view.WebStudioViewMode;
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

/**
 * TODO Refactor
 * 
 * @author snshor
 */
public class WebStudio {
    static interface StudioListener extends EventListener {
        void studioReset();
    }

    private static final Log LOG = LogFactory.getLog(WebStudio.class);

    private WebStudioViewMode DEVELOPER_BYTYPE_VIEW = new DeveloperByTypeViewMode();
    private WebStudioViewMode DEVELOPER_BYFILE_VIEW = new DeveloperByFileViewMode();
    private WebStudioViewMode BUSINESS1_VIEW = new BusinessViewMode1();
    private WebStudioViewMode BUSINESS2_VIEW = new BusinessViewMode2();
    private WebStudioViewMode BUSINESS3_VIEW = new BusinessViewMode3();

    private WebStudioViewMode[] businessModes = { BUSINESS1_VIEW, BUSINESS2_VIEW, BUSINESS3_VIEW };
    private WebStudioViewMode[] developerModes = { DEVELOPER_BYTYPE_VIEW, DEVELOPER_BYFILE_VIEW };

    private String workspacePath;
    private ArrayList<BenchmarkInfo> benchmarks = new ArrayList<BenchmarkInfo>();
    private List<StudioListener> listeners = new ArrayList<StudioListener>();
    private String tableUri;
    private ProjectModel model = new ProjectModel(this);
    private OpenLProjectLocator locator;
    private OpenLWrapperInfo[] wrappers = null;

    private WebStudioViewMode mode = BUSINESS1_VIEW;
    private Set<String> writableProjects;
    private OpenLWrapperInfo currentWrapper;
    private boolean showFormulas;
    private boolean collapseProperties = true;

    private WebStudioProperties properties = new WebStudioProperties();

    private int businessModeIdx = 0;
    private int developerModeIdx = 0;

    public WebStudio() {
        boolean initialized = false;
        try {
            initialized = init();
        } catch (Exception e) {
        }

        if (!initialized) {
            workspacePath = System.getProperty("openl.webstudio.home") == null ? ".." : System
                    .getProperty("openl.webstudio.home");
            locator = new OpenLProjectLocator(workspacePath);
        }
    }

    public WebStudioViewMode[] getViewSubModes(String modeType) {
        WebStudioViewMode[] modes = null;
        if (BaseDeveloperViewMode.TYPE.equals(modeType)) {
            modes = developerModes;
        } else if (BaseBusinessViewMode.TYPE.equals(modeType)) {
            modes = businessModes;
        }
        return modes;
    }

    public WebStudio(String workspacePath) {
        this.workspacePath = workspacePath;
        locator = new OpenLProjectLocator(workspacePath);
    }

    public void addBenchmark(BenchmarkInfo bi) {
        benchmarks.add(0, bi);
    }

    public void addEventListener(StudioListener listener) {
        listeners.add(listener);
    }

    public void executeOperation(String operation, HttpSession session) {
        if ("checkIn".equals(operation)) {
            try {
                UserWorkspaceProject project = getCurrentProject(session);
                if (project == null) {
                    return;
                }
                project.checkIn();
            } catch (Exception e) {
                LOG.error("Can not check in!", e);
            }
        }
        if ("checkOut".equals(operation)) {
            try {
                UserWorkspaceProject project = getCurrentProject(session);
                if (project == null) {
                    return;
                }
                project.checkOut();
            } catch (Exception e) {
                LOG.error("Can not check out!", e);
            }
        }
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
    private int findMode(WebStudioViewMode[] modes, int modeIdx, boolean sameType) {
        if (sameType) {
            modeIdx = (modeIdx + 1) % modes.length;
        }

        mode = modes[modeIdx];
        return modeIdx;
    }

    public BenchmarkInfo[] getBenchmarks() {
        return benchmarks.toArray(new BenchmarkInfo[0]);
    }

    public UserWorkspaceProject getCurrentProject(HttpSession session) {
        if (currentWrapper != null) {
            try {
                String projectName = currentWrapper.getProjectInfo().getName();
                RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
                UserWorkspaceProject project = rulesUserSession.getUserWorkspace().getProject(projectName);
                return project;
            } catch (Exception e) {
                LOG.error("Error when trying to get current project", e);
            }
        }
        return null;
    }

    public UserWorkspaceProject getCurrentProject() {
        return getCurrentProject(FacesUtils.getSession());
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
     * @return Returns the locator.
     */
    public OpenLProjectLocator getLocator() {
        return locator;
    }

    public WebStudioViewMode getMode() {
        return mode;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return Returns the model.
     */
    public ProjectModel getModel() {
        return model;
    }

    public WebStudioProperties getProperties() {
        return properties;
    }

    public String getTableUri() {
        return tableUri;
    }

    /**
     * Returns path on local file system to openL workspace this instance of web
     * studio works with.
     * 
     * @return path to openL projects workspace, i.e. folder containing openL
     *         projects.
     */
    public String getWorkspacePath() {
        return workspacePath;
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

    public boolean init(HttpSession session) {
        UserWorkspace userWorkspace;
        try {
            RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
            userWorkspace = rulesUserSession.getUserWorkspace();
        } catch (WorkspaceException e) {
            LOG.error("Failed to get user workspace", e);
            return false;
        } catch (ProjectException e) {
            LOG.error("Failed to get user workspace", e);
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

    public boolean init() {
        return init(FacesUtils.getSession());
    }

    public void removeBenchmark(int i) {
        benchmarks.remove(i);
    }

    public boolean removeListener(StudioListener listener) {
        return listeners.remove(listener);
    }

    public void reset(ReloadType reloadType) {
        try {
            model.reset(reloadType);
            for (StudioListener listener : listeners) {
                listener.studioReset();
            }
        } catch (Exception e) {
            LOG.error("Error when trying to reset studio model", e);
        }
    }

    public void rebuildModel() {
        reset(ReloadType.RELOAD);
        model.buildProjectTree();
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

    }

    /**
     * DOCUMENT ME!
     * 
     * @param wrapper The currentWrapper to set.
     * 
     * @throws Exception
     */
    public void setCurrentWrapper(OpenLWrapperInfo wrapper) throws Exception {
        if (currentWrapper != wrapper) {
            model.setWrapperInfo(wrapper);
            model.setReadOnly(!((writableProjects == null) || writableProjects.contains(wrapper.getProjectInfo()
                    .getName())));
        }
        currentWrapper = wrapper;
        for (StudioListener listener : listeners) {
            listener.studioReset();
        }
    }

    public void switchMode(String type) throws Exception {
        boolean sameType = type.equals(mode.getType());

        if ("business".equals(type)) {
            businessModeIdx = findMode(businessModes, businessModeIdx, sameType);
        } else if ("developer".equals(type)) {
            developerModeIdx = findMode(developerModes, developerModeIdx, sameType);
        } else {
            throw new RuntimeException("Invalid Mode: " + type);
        }

        model.redraw();
    }

    public void setMode(WebStudioViewMode mode) throws Exception {
        this.mode = mode;
        model.redraw();
    }

    public void setMode(String name) throws Exception {
        WebStudioViewMode mode = getViewMode(name);
        setMode(mode);
    }

    public WebStudioViewMode getViewMode(String name) {
        for (WebStudioViewMode mode : businessModes) {
            if (name.equals(mode.getName())) {
                return mode;
            }
        }
        for (WebStudioViewMode mode : developerModes) {
            if (name.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }

    public void setProperties(WebStudioProperties properties) {
        this.properties = properties;
    }

    public void setTableUri(String tableUri) {
        this.tableUri = tableUri;
    }

    public void setWritableProjects(Set<String> writableProjects) {
        this.writableProjects = writableProjects;
    }

    public boolean isShowFormulas() {
        return showFormulas;
    }

    public void setShowFormulas(String showFormulas) {
        this.showFormulas = Boolean.parseBoolean(showFormulas);
    }

    public boolean isCollapseProperties() {
        return collapseProperties;
    }

    public void setCollapseProperties(String collapseProperties) {
        this.collapseProperties = Boolean.parseBoolean(collapseProperties);
    }
}
