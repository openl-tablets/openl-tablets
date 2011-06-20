/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;

import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.dependencies.ResolvingRulesProjectDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.instantiation.ReloadType;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.runtime.RulesFileDependencyLoader;
import org.openl.rules.ui.view.BaseBusinessViewMode;
import org.openl.rules.ui.view.BaseDeveloperViewMode;
import org.openl.rules.ui.view.BusinessViewMode1;
import org.openl.rules.ui.view.BusinessViewMode2;
import org.openl.rules.ui.view.BusinessViewMode3;
import org.openl.rules.ui.view.DeveloperByFileViewMode;
import org.openl.rules.ui.view.DeveloperByTypeViewMode;
import org.openl.rules.ui.view.WebStudioViewMode;
import org.openl.rules.webstudio.ConfigManager;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.benchmark.BenchmarkInfo;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

/**
 * TODO Remove JSF dependency
 * 
 * @author snshor
 */
public class WebStudio {

    interface StudioListener extends EventListener {
        void studioReset();
    }

    private static final Log LOG = LogFactory.getLog(WebStudio.class);

    public static final String TRACER_NAME = "tracer";

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
    private RulesProjectResolver projectResolver;
    private List<ProjectDescriptor> projects = null;

    private WebStudioViewMode mode = BUSINESS1_VIEW;
    private Module currentModule;
    private boolean showFormulas;
    private boolean collapseProperties = true;

    private WebStudioProperties properties = new WebStudioProperties();

    private int businessModeIdx = 0;
    private int developerModeIdx = 0;

    private RulesProjectDependencyManager dependencyManager;

    private ConfigManager systemConfigManager;
    private boolean needRestart = false;

    public WebStudio(HttpSession session) {
        boolean initialized = false;

        systemConfigManager = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext())
            .getBean(ConfigManager.class);

        try {
            initialized = init(session);
        } catch (Exception e) {
        }

        if (!initialized) {
            workspacePath = systemConfigManager.getStringProperty("webstudio.localWorkspace");
            projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
            projectResolver.setWorkspace(workspacePath);
        }
        initDependencyManager();
    }

    public WebStudio() {
        this(FacesUtils.getSession());
    }
    
    private void initDependencyManager() {
        this.dependencyManager = new RulesProjectDependencyManager();
        
        dependencyManager.setExecutionMode(false);
        
        IDependencyLoader loader1 = new ResolvingRulesProjectDependencyLoader(projectResolver);
        IDependencyLoader loader2 = new RulesFileDependencyLoader();
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));    
        
    }

    public ConfigManager getSystemConfigManager() {
        return systemConfigManager;
    }

    public boolean init(HttpSession session) {
        UserWorkspace userWorkspace = WebStudioUtils.getUserWorkspace(session);

        if (userWorkspace == null) {
            return false;
        }

        workspacePath = userWorkspace.getLocalWorkspace().getLocation().getAbsolutePath();

        projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(workspacePath);

        return true;
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

    public void addBenchmark(BenchmarkInfo bi) {
        benchmarks.add(0, bi);
    }

    public void addEventListener(StudioListener listener) {
        listeners.add(listener);
    }

    public void executeOperation(String operation, HttpSession session) {
        
        if ("checkIn".equals(operation)) {
            try {
                RulesProject project = getCurrentProject(session);
                if (project == null) {
                    return;
                }
                project.checkIn();
            } catch (Exception e) {
                LOG.error("Can not check in!", e);
                try {
                    String redirectLink = String.format("%s/faces/main.xhtml?error=%s", FacesUtils.getContextPath(),
                            e.getMessage());
                    FacesUtils.redirect(redirectLink);
                } catch (IOException e1) {
                    LOG.error("Can`t redirect to with message page", e);
                }
            }
        }
        if ("checkOut".equals(operation)) {
            try {
                RulesProject project = getCurrentProject(session);
                if (project == null) {
                    return;
                }
                project.checkOut();
                reset(ReloadType.FORCED);
            } catch (Exception e) {
                LOG.error("Can not check out!", e);
                try {
                    String redirectLink = String.format("%s/faces/main.xhtml?error=%s", FacesUtils.getContextPath(),
                            e.getMessage());
                    FacesUtils.redirect(redirectLink);
                } catch (IOException e1) {
                    LOG.error("Can`t redirect to with message page", e);
                }
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

    /**
     * TODO Hold current project in session
     * */
    public RulesProject getCurrentProject(HttpSession session) {
        if (currentModule != null) {
            try {
                String projectFolder = currentModule.getProject().getProjectFolder().getName();
                RulesUserSession rulesUserSession = WebStudioUtils.getRulesUserSession(session);
                RulesProject project = rulesUserSession.getUserWorkspace().getProject(projectFolder);
                return project;
            } catch (Exception e) {
                LOG.error("Error when trying to get current project", e);
            }
        }
        return null;
    }

    public RulesProject getCurrentProject() {
        return getCurrentProject(FacesUtils.getSession());
    }

    /**
     * DOCUMENT ME!
     * 
     * @return Returns the current module.
     */
    public Module getCurrentModule() {
        return currentModule;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return Returns the RulesProjectResolver.
     */
    public RulesProjectResolver getProjectResolver() {
        return projectResolver;
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

    public synchronized void invalidateProjects(){
        projects = null;
    }

    public synchronized List<ProjectDescriptor> getAllProjects() {
        if (projects == null) {
            projects = projectResolver.listOpenLProjects();
        }
        return projects;
    }

    public void removeBenchmark(int i) {
        benchmarks.remove(i);
    }

    public boolean removeListener(StudioListener listener) {
        return listeners.remove(listener);
    }

    public void reset(ReloadType reloadType) {
        try {
            if(reloadType == ReloadType.FORCED){
                invalidateProjects();
            }
            model.reset(reloadType);
            for (StudioListener listener : listeners) {
                listener.studioReset();
            }
        } catch (Exception e) {
            LOG.error("Error when trying to reset studio model", e);
        }
    }

    public void rebuildModel() {
        reset(ReloadType.SINGLE);
        model.buildProjectTree();
    }

    public void selectModule(String moduleId) throws Exception {
        if (moduleId == null) {
            if (currentModule != null) {
                return;
            }

            if (getAllProjects().size() > 0) {
                setCurrentModule(getAllProjects().get(0).getModules().get(0));
            }
            return;
        }
        for (ProjectDescriptor project : getAllProjects()) {
            for (Module module : project.getModules()) {
                String curModuleId = getModuleId(module);
                if (curModuleId.equals(moduleId)) {
                    setCurrentModule(module);
                    return;
                }
            }
        }
        if (getAllProjects().size() > 0) {
            setCurrentModule(getAllProjects().get(0).getModules().get(0));
        }
    }

    /**
     * DOCUMENT ME!
     * 
     * @param module The current module to set.
     * 
     * @throws Exception
     */
    public void setCurrentModule(Module module) throws Exception {
        if (currentModule == null
                || !currentModule.getName().equals(module.getName())) {
            model.setModuleInfo(module);
            model.getRecentlyVisitedTables().clear();
        }
        currentModule = module;
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

    public String getModuleId(Module module) {
        if (module != null) {
            return module.getProject().getId() + " - " + module.getName();
        }
        return null;
    }

    public TraceHelper getTraceHelper() {
        TraceHelper traceHelper = (TraceHelper) FacesUtils.getSessionParam(TRACER_NAME);

        if (traceHelper == null) {
            traceHelper = new TraceHelper();
            Map<String, Object> sessionMap = FacesUtils.getSessionMap();
            sessionMap.put(TRACER_NAME, traceHelper);
        }

        return traceHelper;
    }

    public IDependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public void setNeedRestart(boolean needRestart) {
        this.needRestart = needRestart;
    }

    public boolean isNeedRestart() {
        return needRestart;
    }

}
