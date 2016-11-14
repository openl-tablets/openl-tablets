package org.openl.rules.project.resolving;

import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author PUdalau
 */
public class RulesProjectResolver {

    private final Logger log = LoggerFactory.getLogger(RulesProjectResolver.class);

    private String workspace;

    private ISelector<String> projectSelector;
    private ProjectResolver projectResolver;

    public static RulesProjectResolver loadProjectResolverFromClassPath() {
        ProjectResolver projectResolver = ProjectResolver.instance();
        return new RulesProjectResolver(projectResolver);
    }

    private RulesProjectResolver(ProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
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
        return projectResolver.isRulesProject(folder);
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
        Arrays.sort(projects, fileNameComparator);

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

    private static Comparator<File> fileNameComparator = new Comparator<File>() {
        @Override public int compare(File f1, File f2) {
            String name1 = f1.getName();
            String name2 = f2.getName();
            return name1.compareToIgnoreCase(name2);
        }
    };
}
