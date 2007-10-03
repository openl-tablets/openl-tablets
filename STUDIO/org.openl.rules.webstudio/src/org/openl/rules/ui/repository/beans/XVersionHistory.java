package org.openl.rules.ui.repository.beans;

import java.util.LinkedList;
import java.util.List;

public class XVersionHistory {
    private XVersion baseVersion;
    private List<XVersion> versions;
    
    public XVersionHistory() {
	versions = new LinkedList<XVersion>();
    }
    
    public void setBaseVersion(XVersion version) {
	baseVersion = version;
	
	if (!versions.contains(version)) {
	    versions.add(version);
	}
    }
    
    public XVersion getBaseVersion() {
	return baseVersion;
    }
    
    public List<XVersion> getVersion() {
	return versions;
    }
}
