package com.exigen.eclipse.openl.facet.artefact.impl;

import com.exigen.eclipse.common.core.artefact.project.CommonProject;
import com.exigen.eclipse.common.core.artefact.project.facet.CommonFacet;
import com.exigen.eclipse.common.core.artefact.project.facet.CommonFacetFactory;
import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.openl.facet.artefact.OpenLFacet;

public class OpenLFacetFactory implements CommonFacetFactory {
	public CommonFacet createCommonFacet(CommonProject commonProject) throws CommonException {
		return new OpenLFacetImpl(commonProject);
	}

	public Class<? extends CommonFacet> getCommonFacetClass() {
		return OpenLFacet.class;
	}
}
