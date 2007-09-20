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
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;

import com.exigen.common.model.components.java.provider.JavaMethodReturnItemProvider;
import com.exigen.eclipse.common.facet.emf.edit.provider.IItemCreateChildConstraintProvider;
import com.exigen.eclipse.common.facet.emf.edit.provider.IItemPropertyTabSource;
import com.exigen.eclipse.common.facet.emf.infrastructure.dependency.IItemDependencyBuilder;
import com.exigen.eclipse.common.facet.emf.infrastructure.validation.IItemValidator;
import com.exigen.openl.model.openl.OpenlPackage;

/**
 * This is the item provider adapter for a {@link com.exigen.openl.model.openl.RuleSetReturn} object.
 * <!-- begin-user-doc -->
 * @implements IItemCreateChildConstraintProvider
 * <!-- end-user-doc -->
 * @generated
 */
public class RuleSetReturnItemProvider
	extends JavaMethodReturnItemProvider
	implements	
		IEditingDomainItemProvider,	
		IStructuredItemContentProvider,	
		ITreeItemContentProvider,	
		IItemLabelProvider,	
		IItemPropertySource,	
		IItemValidator,	
		IItemPropertyTabSource,	
		IItemDependencyBuilder,
		IItemCreateChildConstraintProvider {

	/**
	 * This constructs an instance from a factory and a notifier.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public RuleSetReturnItemProvider(AdapterFactory adapterFactory) {
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

		}
		return itemPropertyDescriptors;
	}

	/**
	 * This returns RuleSetReturn.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/RuleSetReturn"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated not
	 */
	public String getText(Object object) {
		return super.getText(object);
//		String label = ((RuleSetReturn)object).getDescription();
//		return label == null || label.length() == 0 ?
//			getString("_UI_RuleSetReturn_type") :
//			getString("_UI_RuleSetReturn_type") + " " + label;
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

	public int getMaxOccurs(EObject parent, EReference reference)
	{
		if (OpenlPackage.eINSTANCE.equals(parent.eClass().getEPackage())) {
			return -1;
		}

		return 0;
	}

}
