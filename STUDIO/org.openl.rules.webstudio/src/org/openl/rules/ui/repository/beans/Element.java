package org.openl.rules.ui.repository.beans;

public class Element {
	private String name;
	private String version;
	private String lastModified;
	private String lastModifiedBy;
	
	public Element(String n, String v, String lm, String usr) {
		name = n;
		version = v;
		lastModified = lm;
		lastModifiedBy = usr;
	}

	public String getLastModified() {
		return lastModified;
	}

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}
}
