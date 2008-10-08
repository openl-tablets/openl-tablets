package com.exigen.eclipse.openl.facet.artefact.impl;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.exigen.eclipse.common.core.artefact.framework.Artefact;
import com.exigen.eclipse.common.core.artefact.framework.CompositeArtefact;
import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.core.internal.artefact.project.facet.CommonFacetImpl;
import com.exigen.eclipse.openl.facet.artefact.OpenLFacet;
import com.exigen.eclipse.openl.facet.builder.OpenLModelBuilder;

@SuppressWarnings("restriction")
public class OpenLFacetImpl extends CommonFacetImpl implements OpenLFacet {
	public OpenLFacetImpl(CompositeArtefact parentArtefact)
			throws CommonException {
		super(parentArtefact);
	}

	@Override
	protected Artefact[] getAllArtefactsImpl() throws CommonException {
		return new Artefact[0];
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof OpenLFacetImpl)
				&& ((OpenLFacetImpl) getParent()).equals(getParent());
	}

	public String getProjectFacetId() {
		return FACET_ID;
	}

	public <T extends Artefact> T findArtefact(String name,
			Class<T> artefactClass) throws CommonException {
		return null;
	}

	public String getName() {
		return "OpenL Tablets Facet";
	}

	public Object getUnderlyingResource() {
		return getParent();
	}

	public Artefact getArtefactForResource(IResource resource)
			throws CommonException {
		assertMustBeDiscoverable();

		return null;
	}

	public void install(IProgressMonitor progressMonitor)
			throws CommonException {
		progressMonitor.beginTask("Installing OpenL Tablets facet", 1);
		try {
			getProject().attachBuilderToProject(OpenLModelBuilder.BUILDER_ID,
					new SubProgressMonitor(progressMonitor, 1));
		} finally {
			progressMonitor.done();
		}
	}

	public void uninstall(IProgressMonitor progressMonitor)
			throws CommonException {
		progressMonitor.beginTask("Uninstalling OpenL Tablets facet", 1);
		try {
			getProject().dettachBuilderFromProject(OpenLModelBuilder.BUILDER_ID,
					new SubProgressMonitor(progressMonitor, 1));
		} finally {
			progressMonitor.done();
		}
	}

}
