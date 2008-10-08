package com.exigen.eclipse.openl.facet.artefact;

import org.eclipse.core.runtime.IProgressMonitor;

import com.exigen.eclipse.common.core.artefact.project.facet.CommonFacet;
import com.exigen.eclipse.common.core.exception.CommonException;

public interface OpenLFacet extends CommonFacet {
	static final String FACET_ID = "com.exigen.eclipse.common.facet.emf";

	void install(IProgressMonitor progressMonitor) throws CommonException;

	void uninstall(IProgressMonitor progressMonitor) throws CommonException;

}
