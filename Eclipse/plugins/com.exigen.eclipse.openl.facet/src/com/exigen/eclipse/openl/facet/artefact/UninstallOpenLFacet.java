package com.exigen.eclipse.openl.facet.artefact;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.exigen.eclipse.common.core.CommonCore;
import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.util.Assert;

public class UninstallOpenLFacet implements IDelegate {

	public void execute(IProject project, IProjectFacetVersion fv,
			Object config, IProgressMonitor monitor) throws CoreException {
		try {
			OpenLFacet facet = CommonCore.getWorkspace().getProject(
					project.getName()).getFacet(OpenLFacet.class);
			Assert.isNotNull(facet);

			facet.uninstall(monitor);
		} catch (CommonException e) {
			CommonCore.getDefault().log(e.asErrorStatus());
			throw new CoreException(e.asErrorStatus());
		}
	}

}
