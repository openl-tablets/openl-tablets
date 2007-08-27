/*
 * Created on Jul 10, 2003
 */

package org.openl.eclipse.base;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * @author sam
 *  
 */
public class OpenlNature implements IProjectNature {

	IProject openLProject = null;


	public void configure() throws CoreException {
		
	}

	public void deconfigure() throws CoreException {
		
	}

	public IProject getProject() {
		return openLProject;
	}

	public void setProject(IProject project) {
		openLProject = project;
	}

}