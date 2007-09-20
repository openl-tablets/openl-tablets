/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl.provider;


import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.provider.ComposeableAdapterFactory;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;
import org.eclipse.emf.edit.provider.ItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ViewerNotification;

import com.exigen.common.model.components.ComponentsFactory;
import com.exigen.common.model.primitives.provider.BaseRootModelElementItemProvider;
import com.exigen.eclipse.common.facet.emf.edit.provider.IItemPropertyTabSource;
import com.exigen.eclipse.common.facet.emf.infrastructure.dependency.IItemDependencyBuilder;
import com.exigen.eclipse.common.facet.emf.infrastructure.validation.IItemValidator;
import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.OpenlPackage;
import com.exigen.openl.model.openl.RuleSetFile;

/**
 * This is the item provider adapter for a {@link com.exigen.openl.model.openl.RuleSetFile} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class RuleSetFileItemProvider
	extends BaseRootModelElementItemProvider
	implements	
		IEditingDomainItemProvider,	
		IStructuredItemContentProvider,	
		ITreeItemContentProvider,	
		IItemLabelProvider,	
		IItemPropertySource,	
		IItemValidator,	
		IItemPropertyTabSource,	
		IItemDependencyBuilder {
	/**
	 * This constructs an instance from a factory and a notifier.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RuleSetFileItemProvider(AdapterFactory adapterFactory) {
		super(adapterFactory);
	}

	/**
	 * This returns the property descriptors for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public List getPropertyDescriptors(Object object) {
		if (itemPropertyDescriptors == null) {
			super.getPropertyDescriptors(object);

			addExcelResourceReferencePropertyDescriptor(object);
		}
		return itemPropertyDescriptors;
	}

	/**
	 * This adds a property descriptor for the Excel Resource Reference feature.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void addExcelResourceReferencePropertyDescriptor(Object object) {
		itemPropertyDescriptors.add
			(createItemPropertyDescriptor
				(((ComposeableAdapterFactory)adapterFactory).getRootAdapterFactory(),
				 getResourceLocator(),
				 getString("_UI_RuleSetFile_excelResourceReference_feature"),
				 getString("_UI_PropertyDescriptor_description", "_UI_RuleSetFile_excelResourceReference_feature", "_UI_RuleSetFile_type"),
				 OpenlPackage.Literals.RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE,
				 false,
				 false,
				 false,
				 ItemPropertyDescriptor.GENERIC_VALUE_IMAGE,
				 null,
				 null));
	}

	/**
	 * This specifies how to implement {@link #getChildren} and is used to deduce an appropriate feature for an
	 * {@link org.eclipse.emf.edit.command.AddCommand}, {@link org.eclipse.emf.edit.command.RemoveCommand} or
	 * {@link org.eclipse.emf.edit.command.MoveCommand} in {@link #createCommand}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Collection getChildrenFeatures(Object object) {
		if (childrenFeatures == null) {
			super.getChildrenFeatures(object);
			childrenFeatures.add(OpenlPackage.Literals.RULE_SET_FILE__INIT_PARAMETERS);
			childrenFeatures.add(OpenlPackage.Literals.RULE_SET_FILE__RULE_SETS);
		}
		return childrenFeatures;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EStructuralFeature getChildFeature(Object object, Object child) {
		// Check the type of the specified child object and return the proper feature to use for
		// adding (see {@link AddCommand}) it as a child.

		return super.getChildFeature(object, child);
	}

	/**
	 * This returns RuleSetFile.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/RuleSetFile"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getText(Object object) {
		String label = ((RuleSetFile)object).getExcelResourceReference();
		return label == null || label.length() == 0 ?
			getString("_UI_RuleSetFile_type") :
			getString("_UI_RuleSetFile_type") + " " + label;
	}

	/**
	 * This handles model notifications by calling {@link #updateChildren} to update any cached
	 * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void notifyChanged(Notification notification) {
		updateChildren(notification);

		switch (notification.getFeatureID(RuleSetFile.class)) {
			case OpenlPackage.RULE_SET_FILE__EXCEL_RESOURCE_REFERENCE:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), false, true));
				return;
			case OpenlPackage.RULE_SET_FILE__INIT_PARAMETERS:
			case OpenlPackage.RULE_SET_FILE__RULE_SETS:
				fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, false));
				return;
		}
		super.notifyChanged(notification);
	}

	/**
	 * This adds to the collection of {@link org.eclipse.emf.edit.command.CommandParameter}s
	 * describing all of the children that can be created under this object.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void collectNewChildDescriptors(Collection newChildDescriptors, Object object) {
		super.collectNewChildDescriptors(newChildDescriptors, object);

		newChildDescriptors.add
			(createChildParameter
				(OpenlPackage.Literals.RULE_SET_FILE__INIT_PARAMETERS,
				 ComponentsFactory.eINSTANCE.createInitParameter()));

		newChildDescriptors.add
			(createChildParameter
				(OpenlPackage.Literals.RULE_SET_FILE__RULE_SETS,
				 OpenlFactory.eINSTANCE.createRuleSet()));
	}

	/**
	 * Return the resource locator for this item provider's resources.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ResourceLocator getResourceLocator() {
		return OpenlEditPlugin.INSTANCE;
	}

}
