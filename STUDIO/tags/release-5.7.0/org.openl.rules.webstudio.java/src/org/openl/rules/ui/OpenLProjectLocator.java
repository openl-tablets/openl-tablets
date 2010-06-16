/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.webstudio.util.WebstudioTreeIterator;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.StringTool;
import org.openl.util.tree.TreeIterator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author snshor
 *
 */
public class OpenLProjectLocator {

    private final String workspace;

    private ISelector<String> projectSelector;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        OpenLProjectLocator op = new OpenLProjectLocator("..");
        File[] f = op.listProjects();
        for (int i = 0; i < f.length; i++) {
            System.out.println(f[i].getCanonicalPath() + " - " + op.isRulesProject(f[i]));
            String[] s = op.listPotentialOpenLWrappersClassNames(f[i]);
            for (int j = 0; j < s.length; j++) {
                System.out.println(" ** " + s[j]);
            }
        }
    }

    public OpenLProjectLocator(String workspace) {
        this.workspace = workspace;
    }

    private boolean containsFile(File f, String fname, boolean isDir) throws IOException {
        File ff = new File(f.getCanonicalPath(), fname);
        return ff.exists() && ff.isDirectory() == isDir;

    }

    private boolean containsFileText(File dir, String fname, String content) throws IOException {
        File f = new File(dir.getCanonicalPath(), fname);

        FileReader fr = new FileReader(f);

        BufferedReader br = new BufferedReader(fr);

        try {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.indexOf(content) >= 0) {
                    return true;
                }
            }
            return false;

        } finally {
            br.close();
            fr.close();
        }

    }

    private ISelector<String> getDefaultProjectSelector() {

        String startProject = System.getProperty(IRulesLaunchConstants.START_PROJECT_PROPERTY_NAME);

        return startProject == null ? ASelector.selectAll("") : ASelector.selectObject(startProject);
    }

    public synchronized ISelector<String> getProjectSelector() {
        if (projectSelector == null) {
            projectSelector = getDefaultProjectSelector();
        }
        return projectSelector;
    }

    /**
     * @return Returns the workspace.
     */
    public String getWorkspace() {
        return workspace;
    }

    boolean isRulesProject(File f) throws IOException {
        if (!f.exists() || !f.isDirectory()) {
            return false;
        }

        if (!containsFile(f, ".project", false)) {
            return false;
        }

        if (!containsFileText(f, ".project", "openlbuilder")) {
            return false;
        }
        return true;
    }

    /**
     * @param f
     * @return
     */
    private String javaClassName(File f, String srcroot) {

        String path = f.getPath();
        int inc = 1;
        if (srcroot.endsWith(File.separator)) {
            inc = 0;
        }

        String jpath = path.substring(srcroot.length() + inc, path.length() - 5);
        return jpath.replace(File.separatorChar, '.');

    }

    public List<File> listOpenLFolders() {
        List<File> res = new ArrayList<File>();
        for (File f : listProjects()) {
            try {
                if (isRulesProject(f)) {
                    res.add(f);
                }
            } catch (IOException neverMind) {
            }
        }

        return res;
    }

    public OpenLWrapperInfo[] listOpenLProjects() throws IOException {
        ArrayList<OpenLWrapperInfo> v = new ArrayList<OpenLWrapperInfo>();
        File[] f = listProjects();
        for (int i = 0; i < f.length; i++) {
            // System.out.println(f[i].getCanonicalPath() + " - " +
            // isRulesProject(f[i]));
            if (!isRulesProject(f[i])) {
                continue;
            }

            if (!getProjectSelector().select(f[i].getName())) {
                continue;
            }
            OpenLWebProjectInfo wpi = new OpenLWebProjectInfo(workspace, f[i].getName());
            String[] cc = listPotentialOpenLWrappersClassNames(f[i]);
            for (int j = 0; j < cc.length; j++) {
                // System.out.println(s[j]);
                v.add(new OpenLWrapperInfo(cc[j], wpi));
            }
        }

        return v.toArray(new OpenLWrapperInfo[0]);
    }

    public String[] listPotentialOpenLWrappersClassNames(File project) throws IOException {
        
        List<String> list = new ArrayList<String>();
        
        String startDirs = System.getProperty(IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_PROPERTY, IRulesLaunchConstants.WRAPPER_SEARCH_START_DIR_DEFAULT);
        String wrapperSuffixes = System.getProperty(IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_PROPERTY, IRulesLaunchConstants.WRAPPER_SOURCE_SUFFIX_DEFAULT);
        
        String[] srcRoots = StringTool.tokenize(startDirs, ", ");
        String[] suffixes = StringTool.tokenize(wrapperSuffixes, ", ");
        
        for(String srcRoot: srcRoots)
            listPotentialOpenLWrappersClassNames(project,srcRoot, suffixes, list);
        
        
        return list.toArray(new String[list.size()]);
    }
    
    /**
     * @param project
     * @return
     * @throws IOException
     */
    public void listPotentialOpenLWrappersClassNames(File project, String srcRoot, String[] suffixes, List<String> list) throws IOException {

        File searchDir = new File(project.getCanonicalPath(), srcRoot);
        TreeIterator<File> fti = new WebstudioTreeIterator(searchDir, 0);
        for (; fti.hasNext();) {
            File f = fti.next();
            for(String suffix: suffixes)
                if (f.getName().endsWith(suffix)) {
                    list.add(javaClassName(f, searchDir.getCanonicalPath()));
                }
        }

    }

    File[] listProjects() {

        File wsfolder = new File(workspace);
        return wsfolder.listFiles();

    }

    public void setProjectSelector(ISelector<String> projectSelector) {
        this.projectSelector = projectSelector;
    }
}
