package org.openl.rules.ui.beans;

public class Version {
	private String versionName;
	private String lastModified;
	private String user;
	private String comments;
	
	public Version(String vname, String lastm, String usr, String comm) {
		versionName = vname;
		lastModified = lastm;
		user = usr;
		comments = comm;
	}
	
	public String getVersionName() {
		return versionName;
	}
	
	public String getLastModified() {
		return lastModified;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getComments() {
		return comments;
	}
}
