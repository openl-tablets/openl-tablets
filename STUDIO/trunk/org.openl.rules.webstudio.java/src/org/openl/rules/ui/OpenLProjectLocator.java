/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.rules.ui;

import org.openl.rules.lang.xls.main.IRulesLaunchConstants;
import org.openl.rules.webstudio.util.WebstudioTreeIterator;
import org.openl.util.ASelector;
import org.openl.util.ISelector;
import org.openl.util.TreeIterator;

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
public class OpenLProjectLocator
{

    private final String workspace;

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
	OpenLProjectLocator op = new OpenLProjectLocator("..");
	File[] f = op.listProjects();
	for (int i = 0; i < f.length; i++)
	{
	    System.out.println(f[i].getCanonicalPath() + " - "
		    + op.isRulesProject(f[i]));
	    String[] s = op.listPotentialOpenLWrappersClassNames(f[i]);
	    for (int j = 0; j < s.length; j++)
	    {
		System.out.println(s[j]);
	    }
	}
    }

    public OpenLProjectLocator(String workspace)
    {
	this.workspace = workspace;
    }

    public OpenLWrapperInfo[] listOpenLProjects() throws IOException
    {
	ArrayList<OpenLWrapperInfo> v = new ArrayList<OpenLWrapperInfo>();
	File[] f = listProjects();
	for (int i = 0; i < f.length; i++)
	{
	    // System.out.println(f[i].getCanonicalPath() + " - " +
	    // isRulesProject(f[i]));
	    if (!isRulesProject(f[i]))
		continue;
	    
	    if (!getProjectSelector().select(f[i].getName()))
		continue;
	    OpenLWebProjectInfo wpi = new OpenLWebProjectInfo(workspace, f[i]
		    .getName());
	    String[] cc = listPotentialOpenLWrappersClassNames(f[i]);
	    for (int j = 0; j < cc.length; j++)
	    {
		// System.out.println(s[j]);
		v.add(new OpenLWrapperInfo(cc[j], wpi));
	    }
	}

	return v.toArray(new OpenLWrapperInfo[0]);
    }

    private ISelector<String> projectSelector;

    public synchronized ISelector<String> getProjectSelector()
    {
	if (projectSelector == null)
	    projectSelector = getDefaultProjectSelector();
	return projectSelector;
    }

    private ISelector<String> getDefaultProjectSelector()
    {

	String startProject = System
		.getProperty(IRulesLaunchConstants.START_PROJECT_PROPERTY_NAME);
	
	return startProject == null ? ASelector.selectAll("") : ASelector
		.selectObject(startProject);
    }

    public List<File> listOpenLFolders()
    {
	List<File> res = new ArrayList<File>();
	for (File f : listProjects())
	{
	    try
	    {
		if (isRulesProject(f))
		{
		    res.add(f);
		}
	    } catch (IOException neverMind)
	    {
	    }
	}

	return res;
    }

    /**
     * @param project
     * @return
     * @throws IOException
     */
    public String[] listPotentialOpenLWrappersClassNames(File project)
	    throws IOException
    {

	String srcroot = "gen";
	File searchDir = new File(project.getCanonicalPath(), srcroot);
	TreeIterator<File> fti = new WebstudioTreeIterator(searchDir, 0);
	ArrayList<String> v = new ArrayList<String>();
	for (; fti.hasNext();)
	{
	    File f = (File) fti.next();
	    if (f.getName().endsWith("Wrapper.java"))
		v.add(javaClassName(f, searchDir.getCanonicalPath()));
	}

	return v.toArray(new String[0]);
    }

    /**
     * @param f
     * @return
     */
    private String javaClassName(File f, String srcroot)
    {

	String path = f.getPath();
	int inc = 1;
	if (srcroot.endsWith(File.separator))
	    inc = 0;

	String jpath = path
		.substring(srcroot.length() + inc, path.length() - 5);
	return jpath.replace(File.separatorChar, '.');

    }

    File[] listProjects()
    {

	File wsfolder = new File(workspace);
	return wsfolder.listFiles();

    }

    boolean isRulesProject(File f) throws IOException
    {
	if (!f.exists() || !f.isDirectory())
	    return false;

	if (!containsFile(f, ".project", false))
	    return false;

	if (!containsFileText(f, ".project", "openlbuilder"))
	    return false;
	return true;
    }

    private boolean containsFileText(File dir, String fname, String content)
	    throws IOException
    {
	File f = new File(dir.getCanonicalPath(), fname);

	FileReader fr = new FileReader(f);

	BufferedReader br = new BufferedReader(fr);

	try
	{
	    while (true)
	    {
		String line = br.readLine();
		if (line == null)
		    break;
		if (line.indexOf(content) >= 0)
		    return true;
	    }
	    return false;

	} finally
	{
	    br.close();
	    fr.close();
	}

    }

    private boolean containsFile(File f, String fname, boolean isDir)
	    throws IOException
    {
	File ff = new File(f.getCanonicalPath(), fname);
	return ff.exists() && ff.isDirectory() == isDir;

    }

    /**
     * @return Returns the workspace.
     */
    public String getWorkspace()
    {
	return workspace;
    }

    public void setProjectSelector(ISelector<String> projectSelector)
    {
        this.projectSelector = projectSelector;
    }
}
