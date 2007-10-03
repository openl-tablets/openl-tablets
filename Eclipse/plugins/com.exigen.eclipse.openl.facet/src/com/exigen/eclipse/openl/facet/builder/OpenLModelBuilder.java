package com.exigen.eclipse.openl.facet.builder;

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
import com.exigen.eclipse.common.facet.emf.artefact.DomainFolder;
import com.exigen.eclipse.common.facet.emf.artefact.DomainModel;
import com.exigen.eclipse.common.facet.emf.internal.builder.AbstractBuilder;
import com.exigen.eclipse.openl.facet.Activator;
import com.exigen.eclipse.openl.facet.artefact.OpenLFacet;
import com.exigen.eclipse.openl.facet.util.EclipseOpenLImporter;
import com.exigen.eclipse.openl.model.OpenLModelType;
import com.exigen.openl.component.ErrorListener;
import com.exigen.openl.model.openl.RuleSetFile;

public class OpenLModelBuilder extends AbstractBuilder {

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
		return true;
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
								&& (parentJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT)) {
							try {
								buildNonJavaFile(nonJavaFile, progressMonitor);
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

	private void buildAll(Map args, IProgressMonitor progressMonitor) {
		try {
			JavaFacet javaFacet = currentCommonProject
					.getFacet(JavaFacet.class);
			IPackageFragmentRoot[] allPackageFragmentRoots = javaFacet
					.getCorrespondingJavaProject().getAllPackageFragmentRoots();
			progressMonitor
					.beginTask(
							getBuilderName()
									+ " builder enumerates all Java source folders / libraries",
							allPackageFragmentRoots.length * 2);
			try {
				for (IPackageFragmentRoot packageFragmentRoot : allPackageFragmentRoots) {
					buildNonJavaResources(packageFragmentRoot
							.getNonJavaResources(), new SubProgressMonitor(
							progressMonitor, 1));

					IJavaElement[] children = packageFragmentRoot.getChildren();
					IProgressMonitor enumChildProgress = new SubProgressMonitor(
							progressMonitor, 1);
					enumChildProgress.beginTask(getBuilderName()
							+ " builder enumerates Java packages",
							children.length);
					try {
						for (IJavaElement child : children) {
							if (child instanceof IPackageFragment) {
								IPackageFragment packageFragment = (IPackageFragment) child;
								buildNonJavaResources(packageFragment
										.getNonJavaResources(),
										new SubProgressMonitor(
												enumChildProgress, 1));
							} else {
								enumChildProgress.worked(1);
							}
						}
					} finally {
						enumChildProgress.done();
					}
				}
			} finally {
				progressMonitor.done();
			}
		} catch (CommonException e) {
			ExceptionHandler.handleCommonDesignerExceptionThatMustBeIgnored(e,
					getBuilderName() + " full build is failed");
		} catch (JavaModelException e) {
			ExceptionHandler.handleCoreExceptionThatMustBeIgnored(e,
					getBuilderName() + " full build is failed");
		}
	}

	private void buildNonJavaResources(Object[] nonJavaResources,
			IProgressMonitor progressMonitor) throws CommonException {
		progressMonitor.beginTask(getBuilderName()
				+ " builder enumerates resources", nonJavaResources.length);
		try {
			for (Object nonJavaResource : nonJavaResources) {
				if (nonJavaResource instanceof IFile) {
					buildNonJavaFile((IFile) nonJavaResource,
							new SubProgressMonitor(progressMonitor, 1));
				} else {
					progressMonitor.worked(1);
				}
			}
		} finally {
			progressMonitor.done();
		}
	}

	private void buildNonJavaFile(IFile file, IProgressMonitor progressMonitor)
			throws CommonException {
		if (isFileProcessed(file)) {
			processFile(file, progressMonitor);
		}
	}

	private boolean isFileProcessed(IFile file) throws CommonException {
		return "xls".equals(file.getFileExtension());
	}

	@SuppressWarnings("unchecked")
	private void processFile(final IFile file, IProgressMonitor progressMonitor)
			throws CommonException {
		progressMonitor.beginTask(
				"Create OpenL Tablets model for Excel file \""
						+ file.getFullPath() + "\"", 2);
		try {
			try {
				Artefact artefact = CommonCore.getWorkspace()
						.getArtefactForResource(file.getParent());
				if (artefact instanceof DomainFolder) {
					DomainFolder domainFolder = (DomainFolder) artefact;
					DomainModel domainModel = domainFolder.getDomainModel(file
							.getFullPath().removeFileExtension()
							.addFileExtension(OpenLModelType.FILE_EXTENSION)
							.lastSegment());
					if (domainModel.exists())
						domainModel.delete(new SubProgressMonitor(
								progressMonitor, 1));
					domainModel.create(new SubProgressMonitor(progressMonitor,
							1));

					RuleSetFile ruleSetFile = EclipseOpenLImporter
							.getInstance().importExcelFile(file,
									new ErrorListener() {
										public void bindingError(
												String message,
												Throwable cause, String location) {
											String errorMessage = "BINGING ERROR: "
													+ message;
											IMarker marker = addProblemMarker(
													file,
													errorMessage,
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
											IMarker marker = addProblemMarker(
													file,
													errorMessage,
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
	protected void internalClean(IProgressMonitor monitor)
			throws CommonException {
	}
}
