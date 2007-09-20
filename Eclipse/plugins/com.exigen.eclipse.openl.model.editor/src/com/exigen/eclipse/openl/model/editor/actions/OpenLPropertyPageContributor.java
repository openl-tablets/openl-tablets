package com.exigen.eclipse.openl.model.editor.actions;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.exigen.eclipse.common.core.exception.CommonException;
import com.exigen.eclipse.common.core.internal.ExceptionHandler;
import com.exigen.eclipse.common.facet.emf.edit.provider.ProviderUtils;
import com.exigen.eclipse.common.facet.emf.ui.property.sheet.ModelTabbedPropertyPage;
import com.exigen.eclipse.common.ui.property.page.CommonTabbedPropertyPage;
import com.exigen.eclipse.common.ui.property.page.ICommonPropertyPageContributor;
import com.exigen.openl.model.openl.RuleSet;
import com.exigen.openl.model.openl.RuleSetFile;
import com.exigen.openl.model.openl.presentation.OpenlEditorPlugin;

public class OpenLPropertyPageContributor implements
		ICommonPropertyPageContributor {

	private static final String ACTIONS_GROUP_NAME = "openlActions";

	private CommonTabbedPropertyPage page;

	private GotoExcelRegionActionDelegate gotoExcelRegionActionDelegate;

	private ActionContributionItem gotoExcelRegionActionContributionItem;

	private ISelection selection;

	private boolean isActionsVisible = false;

	private final class SelectionChangedListener implements
			ISelectionChangedListener {
		@SuppressWarnings("unchecked")
		public void selectionChanged(SelectionChangedEvent event) {
			OpenLPropertyPageContributor.this.selection = event.getSelection();
			refreshButtons();
			updateButtonsForCurrentSelection();
		}

		public void dispose() {
		}
	}

	private SelectionChangedListener selectionChangedListener = new SelectionChangedListener();

	private IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {
		public void resourceChanged(IResourceChangeEvent event) {
			updateButtonsForCurrentSelection();
		}
	};

	public void init(CommonTabbedPropertyPage page) {
		this.page = page;
		try {
			ModelTabbedPropertyPage modelTabbedPropertyPage = (ModelTabbedPropertyPage) page;
			this.gotoExcelRegionActionDelegate = new GotoExcelRegionActionDelegate(
					page.getSite().getPage(), modelTabbedPropertyPage
							.getPropertySheetContext().getDomainModel());
		} catch (CommonException e) {
			ExceptionHandler.handleCommonDesignerExceptionThatMustBeIgnored(e);
		}

		this.gotoExcelRegionActionContributionItem = new ActionContributionItem(
				new Action("Go to Excel file", OpenlEditorPlugin.getPlugin()
						.getImageRegistry().getDescriptor(
								OpenlEditorPlugin.Images.LINK_16)) {
					@Override
					public void run() {
						gotoExcelRegionActionDelegate.run(this);
					}
				});

		IToolBarManager toolBarManager = page.getSite().getActionBars()
				.getToolBarManager();
		toolBarManager.add(new Separator(ACTIONS_GROUP_NAME));
		toolBarManager.add(new Separator(ACTIONS_GROUP_NAME + "-end"));

		page.addSelectionChangedListener(selectionChangedListener);

		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener);
	}

	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
		selectionChangedListener.dispose();
		page.removeSelectionChangedListener(selectionChangedListener);
		gotoExcelRegionActionContributionItem.dispose();
	}

	private void refreshButtons() {
		gotoExcelRegionActionDelegate.selectionChanged(
				gotoExcelRegionActionContributionItem.getAction(), selection);

		boolean shouldActionsBeVisible = false;
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedElement = structuredSelection.getFirstElement();
				selectedElement = ProviderUtils
						.unwrapContentObjectToEObject(selectedElement);
				if ((selectedElement instanceof RuleSet)
						|| (selectedElement instanceof RuleSetFile)) {
					shouldActionsBeVisible = true;
				}
			}
		}

		if (shouldActionsBeVisible != isActionsVisible) {
			IToolBarManager toolBarManager = page.getSite().getActionBars()
					.getToolBarManager();

			if (shouldActionsBeVisible) {
				toolBarManager.appendToGroup(ACTIONS_GROUP_NAME,
						gotoExcelRegionActionContributionItem);
			} else {
				toolBarManager.remove(gotoExcelRegionActionContributionItem);
			}
			toolBarManager.update(false);

			isActionsVisible = shouldActionsBeVisible;
		}
	}

	private void updateButtonsForCurrentSelection() {
		RuleSet ruleSet = null;
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			if (structuredSelection.size() == 1) {
				Object selectedElement = structuredSelection.getFirstElement();
				selectedElement = ProviderUtils
						.unwrapContentObjectToEObject(selectedElement);
				if (selectedElement instanceof RuleSet) {
					ruleSet = (RuleSet) selectedElement;
				}
			}
		}
		if (ruleSet != null) {
			updateButtons(ruleSet);
		}
	}

	private void updateButtons(RuleSet ruleSet) {
	}
}
