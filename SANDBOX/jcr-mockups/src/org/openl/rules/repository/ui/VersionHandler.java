package org.openl.rules.repository.ui;

import java.util.LinkedList;
import java.util.List;

import org.openl.rules.repository.ui.beans.Version;

/**
 * Handler for Properties->Versions tab.
 * 
 * @author Aleh Bykhavets
 *
 */
public class VersionHandler {
	private List<Version> versions;
	
	public VersionHandler() {
		versions = new LinkedList<Version>();
		
		versions.add(new Version("1.4", "08/20/2007  4:32pm", "Richard O'Brain", "Ready for Production"));
		versions.add(new Version("1.3", "08/18/2007  1:59am", "Lee Vong", "Verified"));
		versions.add(new Version("1.2", "08/11/2007 11:07am", "Alex T.", "Added improvements for AXA"));
		versions.add(new Version("1.1", "08/05/2007  9:40am", "John S.", "Initial version"));
	}
	
	/**
	 * Gets list of versions for a entity.
	 * 
	 * @return list of versions
	 */
	public List<Version> getVersions() {
		return versions;
	}
}
