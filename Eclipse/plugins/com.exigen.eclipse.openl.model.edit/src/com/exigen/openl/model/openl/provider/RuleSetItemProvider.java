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
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.edit.provider.IEditingDomainItemProvider;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.IItemPropertySource;
import org.eclipse.emf.edit.provider.IStructuredItemContentProvider;
import org.eclipse.emf.edit.provider.ITreeItemContentProvider;

import com.exigen.common.model.components.java.JavaComponentsPackage;
import com.exigen.common.model.components.java.provider.JavaMethodOperationDefinitionItemProvider;
import com.exigen.eclipse.common.facet.emf.edit.provider.FeatureObject;
import com.exigen.eclipse.common.facet.emf.edit.provider.IItemCreateChildConstraintProvider;
import com.exigen.eclipse.common.facet.emf.edit.provider.IItemPropertyTabSource;
import com.exigen.eclipse.common.facet.emf.infrastructure.dependency.IItemDependencyBuilder;
import com.exigen.eclipse.common.facet.emf.infrastructure.validation.IItemValidator;
import com.exigen.eclipse.openl.model.edit.OpenlEditPlugin;
import com.exigen.openl.model.openl.OpenlFactory;
import com.exigen.openl.model.openl.OpenlPackage;
import com.exigen.openl.model.openl.RuleSet;

/**
 * This is the item provider adapter for a {@link com.exigen.openl.model.openl.RuleSet} object.
 * <!-- begin-user-doc -->
 * @implements IItemCreateChildConstraintProvider
 * <!-- end-user-doc -->
 * @generated
 */
public class RuleSetItemProvider
	extends JavaMethodOperationDefinitionItemProvider
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
	public RuleSetItemProvider(AdapterFactory adapterFactory) {
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
	 * This returns RuleSet.gif.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Object getImage(Object object) {
		return overlayImage(object, getResourceLocator().getImage("full/obj16/RuleSet"));
	}

	/**
	 * This returns the label text for the adapted class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getText(Object object) {
		String label = ((RuleSet)object).getName();
		return label == null || label.length() == 0 ?
			getString("_UI_RuleSet_type") :
			getString("_UI_RuleSet_type") + " " + label;
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
	 * @generated not
	 */
	@SuppressWarnings("unchecked")
	protected void collectNewChildDescriptors(Collection newChildDescriptors, Object object) {
//		super.collectNewChildDescriptors(newChildDescriptors, object);

		newChildDescriptors.add
			(createChildParameter
				(JavaComponentsPackage.Literals.JAVA_METHOD_OPERATION_DEFINITION__RETURN,
				 OpenlFactory.eINSTANCE.createRuleSetReturn()));
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

	@Override
	@SuppressWarnings("unchecked")
	protected FeatureObject createWrappingFeatureObject(EObject owner, EStructuralFeature feature)
	{
		if (JavaComponentsPackage.Literals.JAVA_METHOD_OPERATION_DEFINITION__METHOD_PARAMETERS.equals (feature)) {
			return new FeatureObject (getAdapterFactory(), owner, feature) {
				@Override
				protected void collectNewChildDescriptors (Collection newChildDescriptors, Object object) {
					newChildDescriptors.add	(createChildParameter
						(JavaComponentsPackage.Literals.JAVA_METHOD_OPERATION_DEFINITION__METHOD_PARAMETERS,
						 OpenlFactory.eINSTANCE.createRuleSetParameter()));
					newChildDescriptors.add	(createChildParameter
							(JavaComponentsPackage.Literals.JAVA_METHOD_OPERATION_DEFINITION__METHOD_PARAMETERS,
							 OpenlFactory.eINSTANCE.createRuleSetCompositeParameter()));
				}
			};
		}

		return super.createWrappingFeatureObject(owner, feature);
	}

	public int getMaxOccurs(EObject parent, EReference reference)
	{
		if (OpenlPackage.eINSTANCE.equals(parent.eClass().getEPackage())) {
			return -1;
		}

		return 0;
	}

}
