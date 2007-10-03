package com.exigen.eclipse.openl.facet.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.exigen.eclipse.common.core.CommonCore;
import com.exigen.eclipse.common.core.artefact.framework.Artefact;
import com.exigen.eclipse.common.core.artefact.project.facet.JavaFacet;
import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.core.internal.ExceptionHandler;
import com.exigen.eclipse.common.core.internal.builder.AbstractBuilder;
import com.exigen.eclipse.common.facet.emf.artefact.DomainFolder;
import com.exigen.eclipse.common.facet.emf.artefact.DomainModel;
import com.exigen.eclipse.openl.facet.Activator;
import com.exigen.eclipse.openl.facet.artefact.OpenLFacet;
import com.exigen.eclipse.openl.facet.util.EclipseOpenLImporter;
import com.exigen.eclipse.openl.model.OpenLModelType;
import com.exigen.openl.component.ErrorListener;
import com.exigen.openl.model.openl.RuleSetFile;

public class OpenLModelBuilder extends AbstractBuilder {
	public static final String IMPORTER_PROBLEM_MARKER_ID = Activator.PLUGIN_ID
			+ ".OpenLModelBuilder";

	public static final String BUILDER_ID = Activator.PLUGIN_ID
			+ ".OpenLModelBuilder";

	@Override
	protected String getId() {
		return BUILDER_ID;
	}

	@Override
	protected IProject[] getRequiredProjects() {
		return new IProject[0];
	}

	@Override
	protected boolean internalBuild(int kind, Map args,
			IProgressMonitor progressMonitor) throws CommonException {
		if (!currentCommonProject.hasFacet(OpenLFacet.class))
			return false;

		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			buildAll(args, progressMonitor);
		} else {
			IResourceDelta delta = getDelta(currentProject);
			if (delta == null) {
				buildAll(args, progressMonitor);
			} else {
				buildDelta(delta, args, progressMonitor);
			}
		}
		needRebuild();
		return true;
	}

	private void buildAll(Map args, IProgressMonitor progressMonitor) throws CommonException {
		Collection<IFile> files = new ArrayList<IFile>();
		progressMonitor.beginTask(
				getBuilderName()
						+ " builder enumerates all all Excel files",
				1);
		try{
			collectFiles(files);
		}finally{
			progressMonitor.done();
		}
		progressMonitor.beginTask(getBuilderName()+ " builder process files ",files.size());
		try{
			for (IFile file : files) {
				processFile(file, new SubProgressMonitor(progressMonitor,1));
				
			}
		}finally{
			progressMonitor.done();
		}
		
	}
	

	private void buildDelta(IResourceDelta delta, Map args,
			final IProgressMonitor progressMonitor) {
		
		try {
			delta.accept(new IResourceDeltaVisitor() {
				public boolean visit(IResourceDelta delta) throws CoreException {
					if (delta.getResource().getType() == IResource.FILE) {
						IFile nonJavaFile = (IFile) delta.getResource();
						IJavaElement parentJavaElement = JavaCore
								.create(nonJavaFile.getParent());
						if ((parentJavaElement != null)
								&& ((parentJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) || (parentJavaElement
										.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT))) {
							try {
								if (delta.getKind()==IResourceDelta.REMOVED)
									cleanFile(nonJavaFile, progressMonitor);
								else
									processFile(nonJavaFile, progressMonitor);
							} catch (CommonException e) {
								ExceptionHandler.wrapIntoCoreException(e);
							}
						}
					}
					return true;
				}
			});
		} catch (CoreException e) {
			ExceptionHandler.handleCoreExceptionThatMustBeIgnored(e,
					getBuilderName() + " incremental build is failed");
		}
	}

	protected String getBuilderName() {
		return "OpenL Tablets";
	}

	private boolean isFileProcessed(IFile file)  {
		return "xls".equals(file.getFileExtension());
	}

	private void collectFiles(Collection<IFile> files) {
		try {
			JavaFacet javaFacet = currentCommonProject
					.getFacet(JavaFacet.class);
			IPackageFragmentRoot[] allPackageFragmentRoots = javaFacet
					.getCorrespondingJavaProject().getAllPackageFragmentRoots();

			for (IPackageFragmentRoot packageFragmentRoot : allPackageFragmentRoots) {

				collectNonJavaResources(files, packageFragmentRoot.getNonJavaResources());
				IJavaElement[] children = packageFragmentRoot.getChildren();

				for (IJavaElement child : children) {
					if (child instanceof IPackageFragment) {
						IPackageFragment packageFragment = (IPackageFragment) child;
						collectNonJavaResources(files, packageFragment
								.getNonJavaResources());
					}
					
				}

			}

		} catch (CommonException e) {
			ExceptionHandler.handleCommonDesignerExceptionThatMustBeIgnored(e,
					getBuilderName() + " full build is failed");
		} catch (JavaModelException e) {
			ExceptionHandler.handleCoreExceptionThatMustBeIgnored(e,
					getBuilderName() + " full build is failed");
		}
	}


	private void collectNonJavaResources(Collection<IFile> files, Object[] nonJavaResources) {
		for (Object nonJavaResource : nonJavaResources) {
			if (nonJavaResource instanceof IFile) {
				collectNonJavaFile(files,(IFile) nonJavaResource);
			}
		}
	}

	private void collectNonJavaFile(Collection<IFile> files,IFile file) {
		if (isFileProcessed(file)) 
			files.add(file);
		
	}
	@SuppressWarnings("unchecked")
	private void cleanFile(final IFile file, IProgressMonitor progressMonitor)
			throws CommonException {
		progressMonitor.beginTask(
				"Delete OpenL Tablets model for Excel file \""
						+ file.getFullPath() + "\"", 2);
		try {
			try {
				if (file.exists())
					cleanProblemMarkers(file);
				DomainModel domainModel = getGeneratedDomainModel(file);
				if (domainModel!=null){
					if (domainModel.exists())
						domainModel.getCorrespondingFile().delete(true,null);
				}
			} catch (Exception e) {
				ExceptionHandler
						.handleSystemExceptionThatMustBeIgnored(e,
								"Cannot import clean import results for Excel file with OpenL Tablets");
			}
		} finally {
			progressMonitor.done();
		}
	}
	
	protected DomainModel getGeneratedDomainModel(IFile file) throws CommonException {
		Artefact artefact = CommonCore.getWorkspace().getArtefactForResource(
				file.getParent());
		if (artefact instanceof DomainFolder) {
			DomainFolder domainFolder = (DomainFolder) artefact;
			DomainModel domainModel = domainFolder.getDomainModel(file
					.getFullPath().removeFileExtension().addFileExtension(
							OpenLModelType.FILE_EXTENSION).lastSegment());
			return domainModel;
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private void processFile(final IFile file, IProgressMonitor progressMonitor) throws CommonException 
			{
		progressMonitor.beginTask(
				"Create OpenL Tablets model for Excel file \""
						+ file.getFullPath() + "\"", 2);
		try {
			try {
				cleanProblemMarkers(file);
				
				DomainModel domainModel = getGeneratedDomainModel(file);
				if (domainModel!=null){
					if (!domainModel.exists())
						domainModel.create(new SubProgressMonitor(
								progressMonitor, 1));

					RuleSetFile ruleSetFile = EclipseOpenLImporter
							.getInstance().importExcelFile(file,
									new ErrorListener() {
										public void bindingError(
												String message,
												Throwable cause, String location) {
											String errorMessage = "BINDING ERROR: "
													+ message;
											IMarker marker = addImporterProblemMarker(
													file, errorMessage,
													IMarker.SEVERITY_ERROR);
											try {
												marker.setAttribute(
														IMarker.LOCATION,
														location);
											} catch (CoreException e) {
												ExceptionHandler
														.handleCoreExceptionThatMustBeIgnored(
																e,
																"Unable to add marker");

											}
											if (cause != null) {
												ExceptionHandler
														.handleSystemExceptionThatMustBeIgnored(
																cause,
																errorMessage);
											}
										}

										public void parsingError(
												String message,
												Throwable cause, String location) {
											String errorMessage = "PARSING ERROR: "
													+ message;
											IMarker marker = addImporterProblemMarker(
													file, errorMessage,
													IMarker.SEVERITY_ERROR);
											try {
												marker.setAttribute(
														IMarker.LOCATION,
														location);
											} catch (CoreException e) {
												ExceptionHandler
														.handleCoreExceptionThatMustBeIgnored(
																e,
																"Unable to add marker");

											}
											if (cause != null) {
												ExceptionHandler
														.handleSystemExceptionThatMustBeIgnored(
																cause,
																errorMessage);
											}
										}
									});

					if (ruleSetFile != null) {
						domainModel.getCorrespondingEmfResource().getContents()
								.clear();
						domainModel.getCorrespondingEmfResource().getContents()
								.add(ruleSetFile);

						domainModel.save();
					}
				}
			} catch (Exception e) {
				ExceptionHandler.handleSystemExceptionThatCannotBeIgnored(e,
						"Cannot import Excel file with OpenL Tablets");
			}
		} finally {
			progressMonitor.done();
		}
	}

	@Override
	protected void internalClean(IProgressMonitor progressMonitor)
			throws CommonException {
		Collection<IFile> files = new ArrayList<IFile>();
		progressMonitor.beginTask(
				getBuilderName()
						+ " builder enumerates all Excel files",
				1);
		try{
			collectFiles(files);
		}finally{
			progressMonitor.done();
		}
		progressMonitor.beginTask(getBuilderName()+ " builder process files ",files.size());
		try{
			for (IFile file : files) {
				cleanFile(file, new SubProgressMonitor(progressMonitor,1));
				
			}
		}finally{
			progressMonitor.done();
		}
	}

	private IMarker addImporterProblemMarker(IResource resource,
			String errorMessage, int severity_error) {
		try {
			IMarker marker = resource.createMarker(IMPORTER_PROBLEM_MARKER_ID);
			marker.setAttribute(IMarker.MESSAGE, errorMessage);
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			return marker;
		} catch (CoreException e) {
			ExceptionHandler.handleCoreExceptionThatMustBeIgnored(e,
					"Unable to add marker");
		}
		return null;
	}

	private void cleanProblemMarkers(IResource resource) throws CoreException {
		resource.deleteMarkers(IMPORTER_PROBLEM_MARKER_ID, true,
				IResource.DEPTH_INFINITE);

	}
}
