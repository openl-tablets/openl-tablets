/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl.util;

import com.exigen.common.model.components.BaseNamedParameter;
import com.exigen.common.model.components.BaseParameter;
import com.exigen.common.model.components.ComponentDefinition;
import com.exigen.common.model.components.OperationDefinition;

import com.exigen.common.model.components.Parameter;

import com.exigen.common.model.components.java.JavaMethodOperationDefinition;

import com.exigen.common.model.components.java.JavaMethodParameter;
import com.exigen.common.model.components.java.JavaMethodReturn;

import com.exigen.common.model.primitives.BaseElement;
import com.exigen.common.model.primitives.BaseInheritableElement;
import com.exigen.common.model.primitives.BaseNamedElement;
import com.exigen.common.model.primitives.BaseRootModelElement;
import com.exigen.common.model.primitives.NamedElement;
import com.exigen.common.model.primitives.ReferencableElement;
import com.exigen.common.model.primitives.RootModelElement;

import com.exigen.openl.model.openl.*;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see com.exigen.openl.model.openl.OpenlPackage
 * @generated
 */
public class OpenlAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static OpenlPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OpenlAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = OpenlPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch the delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected OpenlSwitch modelSwitch =
		new OpenlSwitch() {
			public Object caseRuleSetFile(RuleSetFile object) {
				return createRuleSetFileAdapter();
			}
			public Object caseRuleSet(RuleSet object) {
				return createRuleSetAdapter();
			}
			public Object caseRuleSetParameter(RuleSetParameter object) {
				return createRuleSetParameterAdapter();
			}
			public Object caseRuleSetReturn(RuleSetReturn object) {
				return createRuleSetReturnAdapter();
			}
			public Object caseBaseElement(BaseElement object) {
				return createBaseElementAdapter();
			}
			public Object caseRootModelElement(RootModelElement object) {
				return createRootModelElementAdapter();
			}
			public Object caseBaseRootModelElement(BaseRootModelElement object) {
				return createBaseRootModelElementAdapter();
			}
			public Object caseReferencableElement(ReferencableElement object) {
				return createReferencableElementAdapter();
			}
			public Object caseComponentDefinition(ComponentDefinition object) {
				return createComponentDefinitionAdapter();
			}
			public Object caseNamedElement(NamedElement object) {
				return createNamedElementAdapter();
			}
			public Object caseBaseNamedElement(BaseNamedElement object) {
				return createBaseNamedElementAdapter();
			}
			public Object caseOperationDefinition(OperationDefinition object) {
				return createOperationDefinitionAdapter();
			}
			public Object caseJavaMethodOperationDefinition(JavaMethodOperationDefinition object) {
				return createJavaMethodOperationDefinitionAdapter();
			}
			public Object caseParameter(Parameter object) {
				return createParameterAdapter();
			}
			public Object caseBaseParameter(BaseParameter object) {
				return createBaseParameterAdapter();
			}
			public Object caseBaseNamedParameter(BaseNamedParameter object) {
				return createBaseNamedParameterAdapter();
			}
			public Object caseJavaMethodParameter(JavaMethodParameter object) {
				return createJavaMethodParameterAdapter();
			}
			public Object caseJavaMethodReturn(JavaMethodReturn object) {
				return createJavaMethodReturnAdapter();
			}
			public Object defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	public Adapter createAdapter(Notifier target) {
		return (Adapter)modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.openl.model.openl.RuleSetFile <em>Rule Set File</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.openl.model.openl.RuleSetFile
	 * @generated
	 */
	public Adapter createRuleSetFileAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.openl.model.openl.RuleSet <em>Rule Set</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.openl.model.openl.RuleSet
	 * @generated
	 */
	public Adapter createRuleSetAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.openl.model.openl.RuleSetParameter <em>Rule Set Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.openl.model.openl.RuleSetParameter
	 * @generated
	 */
	public Adapter createRuleSetParameterAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.openl.model.openl.RuleSetReturn <em>Rule Set Return</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.openl.model.openl.RuleSetReturn
	 * @generated
	 */
	public Adapter createRuleSetReturnAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.BaseElement <em>Base Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.BaseElement
	 * @generated
	 */
	public Adapter createBaseElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.RootModelElement <em>Root Model Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.RootModelElement
	 * @generated
	 */
	public Adapter createRootModelElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.BaseRootModelElement <em>Base Root Model Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.BaseRootModelElement
	 * @generated
	 */
	public Adapter createBaseRootModelElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.ReferencableElement <em>Referencable Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.ReferencableElement
	 * @generated
	 */
	public Adapter createReferencableElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.ComponentDefinition <em>Component Definition</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.ComponentDefinition
	 * @generated
	 */
	public Adapter createComponentDefinitionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.NamedElement <em>Named Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.NamedElement
	 * @generated
	 */
	public Adapter createNamedElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.primitives.BaseNamedElement <em>Base Named Element</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.primitives.BaseNamedElement
	 * @generated
	 */
	public Adapter createBaseNamedElementAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.OperationDefinition <em>Operation Definition</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.OperationDefinition
	 * @generated
	 */
	public Adapter createOperationDefinitionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.java.JavaMethodOperationDefinition <em>Java Method Operation Definition</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.java.JavaMethodOperationDefinition
	 * @generated
	 */
	public Adapter createJavaMethodOperationDefinitionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.Parameter <em>Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.Parameter
	 * @generated
	 */
	public Adapter createParameterAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.BaseParameter <em>Base Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.BaseParameter
	 * @generated
	 */
	public Adapter createBaseParameterAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.BaseNamedParameter <em>Base Named Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.BaseNamedParameter
	 * @generated
	 */
	public Adapter createBaseNamedParameterAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.java.JavaMethodParameter <em>Java Method Parameter</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.java.JavaMethodParameter
	 * @generated
	 */
	public Adapter createJavaMethodParameterAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link com.exigen.common.model.components.java.JavaMethodReturn <em>Java Method Return</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see com.exigen.common.model.components.java.JavaMethodReturn
	 * @generated
	 */
	public Adapter createJavaMethodReturnAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //OpenlAdapterFactory
