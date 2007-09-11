package org.openl.rules.repository.lwspace;

import java.io.File;

import org.openl.rules.repository.jcr.JcrProject;

public interface LocalWorkspace {
	/**
	 * Initializes local workspace.
	 * At first it cleans temporarily folder from leftovers, if any.
	 * Then it downloads files from JCR.
	 * 
	 * @param project JCR project
	 * @param tempLocation temporarily folder for project files
	 */
	public void initialize(JcrProject project, File tempLocation);
	/**
	 * Cleans the local workspace.
	 * All files in temporarily  
	 *
	 */
	public void clean();
}
