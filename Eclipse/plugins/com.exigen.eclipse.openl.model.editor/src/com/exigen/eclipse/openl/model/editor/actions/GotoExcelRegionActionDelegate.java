package com.exigen.eclipse.openl.model.editor.actions;

import java.lang.reflect.Method;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.OleClientSite;
import org.eclipse.swt.ole.win32.Variant;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IDE;

import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.facet.emf.CommonFacetEmf;
import com.exigen.eclipse.common.facet.emf.artefact.DomainModel;
import com.exigen.eclipse.common.facet.emf.artefact.ModelingFacet;
import com.exigen.eclipse.common.facet.emf.edit.provider.ProviderUtils;
import com.exigen.eclipse.common.facet.emf.lookup.LookupScope;
import com.exigen.eclipse.common.facet.emf.manager.ModelState;
import com.exigen.eclipse.common.ui.util.exception.UiExceptionHandler;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;

public class GotoExcelRegionActionDelegate implements IActionDelegate {
	private IWorkbenchPage workbenchPage;

	private Object selectedObject;

	private DomainModel domainModel;

	public GotoExcelRegionActionDelegate(IWorkbenchPage workbenchPage,
			DomainModel domainModel) {
		this.workbenchPage = workbenchPage;
		this.domainModel = domainModel;
	}

	public void run(IAction action) {
		if (!(selectedObject instanceof RuleSet))
			return;
		RuleSet ruleSet = (RuleSet) selectedObject;
		RuleSetFile ruleSetFile = (RuleSetFile) ruleSet.eContainer();
		try {
			ModelingFacet modelingFacet = domainModel.getProject().getFacet(
					ModelingFacet.class);
			LookupScope scope = (modelingFacet.getCommonResourceSet()
					.getCommonURIConverter().getLookupScope());
			ModelState modelState = CommonFacetEmf.getModelManager().getState(
					scope);
			DomainModel excelFileModel = modelState.getModelByUri(URI
					.createURI(ruleSetFile.getExcelResourceReference()));
			if ((excelFileModel == null) || !excelFileModel.exists()) {
				throw new CommonException("File \""
						+ ruleSetFile.getExcelResourceReference()
						+ "\" is not found");
			}

			IFile file = excelFileModel.getCorrespondingFile();
			IEditorPart editor = IDE.openEditor(workbenchPage, file,
					"org.eclipse.ui.systemInPlaceEditor");
			if (editor.getClass().getName().equals(
					"org.eclipse.ui.internal.editorsupport.win32.OleEditor")) {
				Method method = editor.getClass().getMethod("getClientSite",
						new Class[0]);
				OleClientSite oleClientSite = (OleClientSite) method.invoke(
						editor, new Object[0]);
				if (oleClientSite == null) {
					throw new CommonException("Cannot access Excell editor");
				}

				OleAutomation oleAutomation = new OleAutomation(oleClientSite);
				try {
					int[] dispIdArray = oleAutomation
							.getIDsOfNames(new String[] { "Application" });
					Variant application = oleAutomation
							.getProperty(dispIdArray[0]);
					try {
						dispIdArray = application.getAutomation()
								.getIDsOfNames(
										new String[] { "Goto", "Reference",
												"Scroll" });
						application.getAutomation().invoke(
								dispIdArray[0],
								new Variant[] { new Variant(ruleSet.getName()),
										new Variant(true) });
					} finally {
						application.dispose();
					}
				} finally {
					oleAutomation.dispose();
				}
			}
		} catch (CommonException e) {
			UiExceptionHandler.handleCommonExceptionOnUserAction(e,
					"Cannot open Excel editor");
		} catch (Exception e) {
			UiExceptionHandler.handleCommonExceptionOnUserAction(e,
					"Cannot open Excel editor");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			selectedObject = structuredSelection.getFirstElement();
			EObject unwrappedObject = ProviderUtils
					.unwrapContentObjectToEObject(selectedObject);
			if (unwrappedObject != null) {
				selectedObject = unwrappedObject;
			}
		} else {
			selectedObject = null;
		}
	}
}
