package org.openl.rules.ui.repository.beans;

import java.util.LinkedList;
import java.util.List;

public class XRepository {
    private List<XProject> projects;
    
    public List<XProject> listProjects() {
	if (projects == null) {
	    init();
	}
	
	return projects;
    }
    
    private void init() {
	projects = new LinkedList<XProject>();
	
	XUser user1 = new XUser("John S."); 
	XUser user2 = new XUser("Alex T.");
	XUser user3 = new XUser("Lee Vong");
	XUser user4 = new XUser("Richard O'Brain");
	
	XProject prj1 = new XProject("/test/prj1", "prj1");
	{
	    prj1.getVersionHistory().setBaseVersion(new XVersion("1.0", "2007/10/01", user1, "c10"));
	    prj1.getVersionHistory().setBaseVersion(new XVersion("1.1", "2007/10/02", user1, "c11"));
	    prj1.getVersionHistory().setBaseVersion(new XVersion("1.2", "2007/10/03", user2, "c12"));
	    XFolder r = prj1.getRootFolder();
	    r.addFolder(new XFolder("bin"));
	    r.addFolder(new XFolder("build"));
	    r.addFolder(new XFolder("docs"));
	    XFolder fRules = new XFolder("rules");
	    fRules.addFile(new XFile("test1.xls"));
	    fRules.addFile(new XFile("test2.xls"));
	    r.addFolder(fRules);
	}
	add(prj1);
	
	XProject prj2 = new XProject("/test/prj2", "prj2");
	{
	    prj2.getVersionHistory().setBaseVersion(new XVersion("1.0", "2007/10/02", user2, "c20"));
	    prj2.getVersionHistory().setBaseVersion(new XVersion("1.1", "2007/10/03", user2, "c21"));
	    prj2.getVersionHistory().setBaseVersion(new XVersion("1.2", "2007/10/04", user2, "c22"));
	    XFolder r = prj1.getRootFolder();
	    r.addFolder(new XFolder("bin"));
	    r.addFolder(new XFolder("build"));
	    r.addFolder(new XFolder("docs"));
	    r.addFolder(new XFolder("rules"));
	}
	add(prj2);

	XProject prj3 = new XProject("/test/prj3", "prj3");
	{
	    prj3.getVersionHistory().setBaseVersion(new XVersion("1.0", "2007/10/03", user3, "c30"));
	    prj3.getVersionHistory().setBaseVersion(new XVersion("1.1", "2007/10/04", user3, "c31"));
	    prj3.getVersionHistory().setBaseVersion(new XVersion("1.2", "2007/10/05", user3, "c32"));
	    prj3.getVersionHistory().setBaseVersion(new XVersion("1.3", "2007/10/06", user3, "c33"));
	    prj3.getVersionHistory().setBaseVersion(new XVersion("1.4", "2007/10/07", user4, "c34"));
	    XFolder r = prj1.getRootFolder();
	    r.addFolder(new XFolder("bin"));
	    r.addFolder(new XFolder("build"));
	    r.addFolder(new XFolder("docs"));
	    r.addFolder(new XFolder("rules"));
	}
	add(prj3);
    }
    
    private void add(XProject project) {
	projects.add(project);
	project.setParent(this);
    }
    
    protected void delete(XProject project) {
	projects.remove(project);
    }
}
