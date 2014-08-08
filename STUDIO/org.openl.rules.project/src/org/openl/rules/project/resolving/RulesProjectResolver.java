package org.openl.rules.project.resolving;

import org.apache.commons.io.comparator.NameFileComparator;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author PUdalau
 */
public class RulesProjectResolver {

    private final Logger log = LoggerFactory.getLogger(RulesProjectResolver.class);


    private List<ResolvingStrategy> resolvingStrategies;

    private String workspace;

    private ISelector<String> projectSelector;

    public static RulesProjectResolver loadProjectResolverFromClassPath() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "project-resolver-beans.xml");
        return (RulesProjectResolver) applicationContext.getBean("projectResolver");
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public List<ResolvingStrategy> getResolvingStrategies() {
        return resolvingStrategies;
    }

    public void setResolvingStrategies(List<ResolvingStrategy> resolvingStrategies) {
        this.resolvingStrategies = resolvingStrategies;
    }

    public void setProjectSelector(ISelector<String> projectSelector) {
        this.projectSelector = projectSelector;
    }

    /**
     * @return All OpenLProjects in workspace that was be selected(See
     * {@link ISelector}).
     */
    public List<ProjectDescriptor> listOpenLProjects() {
        List<ProjectDescriptor> projects = new ArrayList<ProjectDescriptor>();
        File[] f = listProjects();
        for (int i = 0; i < f.length; i++) {
            try {
                if (!getProjectSelector().select(f[i].getName())) {
                    continue;
                }
                ResolvingStrategy strategy = isRulesProject(f[i]);
                if (strategy != null) {
                    projects.add(strategy.resolveProject(f[i]));
                }
            } catch (Exception e) {
                Log.warn(String.format("Failed to resolve project in [%s]", f[i].getAbsoluteFile()), e);
            }
        }

        return projects;
    }

    /**
     * @param folder Folder to check
     * @return <code>null</code> if it is not OpenL project and
     * {@link ResolvingStrategy} for this project otherwise.
     */
    public ResolvingStrategy isRulesProject(File folder) {
        if (resolvingStrategies == null) {
            throw new OpenlNotCheckedException("Resolving strategies weren't set.");
        }
        for (ResolvingStrategy strategy : resolvingStrategies) {
            if (strategy.isRulesProject(folder)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * @return Selector that will define which projects have to be resolved and
     * which have to be skipped
     */
    public synchronized ISelector<String> getProjectSelector() {
        if (projectSelector == null) {
            projectSelector = getDefaultProjectSelector();
        }
        return projectSelector;
    }

    private ISelector<String> getDefaultProjectSelector() {
        String startProject = System.getProperty(IRulesLaunchConstants.START_PROJECT_PROPERTY_NAME);
        return startProject == null ? ASelector.selectAll("") : ASelector.selectObject(startProject);
    }

    private File[] listProjects() {
        File wsfolder = new File(workspace);
        if (!wsfolder.exists())
            log.error("Workspace Folder {} does not exist!", wsfolder.getAbsolutePath());

        File[] projects = wsfolder.listFiles();
        Arrays.sort(projects, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

        return projects;
    }

    /**
     * @return All OpenL projects folders in workspace
     */
    public List<File> listOpenLFolders() {
        List<File> res = new ArrayList<File>();
        for (File f : listProjects()) {
            if (isRulesProject(f) != null) {
                res.add(f);
            }
        }
        return res;
    }
}
