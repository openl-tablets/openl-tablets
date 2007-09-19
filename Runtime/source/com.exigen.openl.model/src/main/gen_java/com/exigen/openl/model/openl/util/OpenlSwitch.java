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
import com.exigen.common.model.primitives.BaseNamedElement;
import com.exigen.common.model.primitives.BaseRootModelElement;
import com.exigen.common.model.primitives.NamedElement;
import com.exigen.common.model.primitives.ReferencableElement;
import com.exigen.common.model.primitives.RootModelElement;

import com.exigen.openl.model.openl.*;

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Switch</b> for the model's inheritance hierarchy.
 * It supports the call {@link #doSwitch(EObject) doSwitch(object)}
 * to invoke the <code>caseXXX</code> method for each class of the model,
 * starting with the actual class of the object
 * and proceeding up the inheritance hierarchy
 * until a non-null result is returned,
 * which is the result of the switch.
 * <!-- end-user-doc -->
 * @see com.exigen.openl.model.openl.OpenlPackage
 * @generated
 */
public class OpenlSwitch {
	/**
	 * The cached model package
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static OpenlPackage modelPackage;

	/**
	 * Creates an instance of the switch.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OpenlSwitch() {
		if (modelPackage == null) {
			modelPackage = OpenlPackage.eINSTANCE;
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	public Object doSwitch(EObject theEObject) {
		return doSwitch(theEObject.eClass(), theEObject);
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected Object doSwitch(EClass theEClass, EObject theEObject) {
		if (theEClass.eContainer() == modelPackage) {
			return doSwitch(theEClass.getClassifierID(), theEObject);
		}
		else {
			List eSuperTypes = theEClass.getESuperTypes();
			return
				eSuperTypes.isEmpty() ?
					defaultCase(theEObject) :
					doSwitch((EClass)eSuperTypes.get(0), theEObject);
		}
	}

	/**
	 * Calls <code>caseXXX</code> for each class of the model until one returns a non null result; it yields that result.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the first non-null result returned by a <code>caseXXX</code> call.
	 * @generated
	 */
	protected Object doSwitch(int classifierID, EObject theEObject) {
		switch (classifierID) {
			case OpenlPackage.RULE_SET_FILE: {
				RuleSetFile ruleSetFile = (RuleSetFile)theEObject;
				Object result = caseRuleSetFile(ruleSetFile);
				if (result == null) result = caseBaseRootModelElement(ruleSetFile);
				if (result == null) result = caseComponentDefinition(ruleSetFile);
				if (result == null) result = caseBaseElement(ruleSetFile);
				if (result == null) result = caseRootModelElement(ruleSetFile);
				if (result == null) result = caseReferencableElement(ruleSetFile);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case OpenlPackage.RULE_SET: {
				RuleSet ruleSet = (RuleSet)theEObject;
				Object result = caseRuleSet(ruleSet);
				if (result == null) result = caseJavaMethodOperationDefinition(ruleSet);
				if (result == null) result = caseBaseNamedElement(ruleSet);
				if (result == null) result = caseOperationDefinition(ruleSet);
				if (result == null) result = caseBaseElement(ruleSet);
				if (result == null) result = caseNamedElement(ruleSet);
				if (result == null) result = caseReferencableElement(ruleSet);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case OpenlPackage.RULE_SET_PARAMETER: {
				RuleSetParameter ruleSetParameter = (RuleSetParameter)theEObject;
				Object result = caseRuleSetParameter(ruleSetParameter);
				if (result == null) result = caseJavaMethodParameter(ruleSetParameter);
				if (result == null) result = caseBaseNamedParameter(ruleSetParameter);
				if (result == null) result = caseBaseNamedElement(ruleSetParameter);
				if (result == null) result = caseBaseParameter(ruleSetParameter);
				if (result == null) result = caseBaseElement(ruleSetParameter);
				if (result == null) result = caseNamedElement(ruleSetParameter);
				if (result == null) result = caseParameter(ruleSetParameter);
				if (result == null) result = caseReferencableElement(ruleSetParameter);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			case OpenlPackage.RULE_SET_RETURN: {
				RuleSetReturn ruleSetReturn = (RuleSetReturn)theEObject;
				Object result = caseRuleSetReturn(ruleSetReturn);
				if (result == null) result = caseJavaMethodReturn(ruleSetReturn);
				if (result == null) result = caseBaseElement(ruleSetReturn);
				if (result == null) result = caseBaseParameter(ruleSetReturn);
				if (result == null) result = caseParameter(ruleSetReturn);
				if (result == null) result = caseReferencableElement(ruleSetReturn);
				if (result == null) result = defaultCase(theEObject);
				return result;
			}
			default: return defaultCase(theEObject);
		}
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Rule Set File</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Rule Set File</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRuleSetFile(RuleSetFile object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Rule Set</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Rule Set</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRuleSet(RuleSet object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Rule Set Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Rule Set Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRuleSetParameter(RuleSetParameter object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Rule Set Return</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Rule Set Return</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRuleSetReturn(RuleSetReturn object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Base Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Base Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseBaseElement(BaseElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Root Model Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Root Model Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseRootModelElement(RootModelElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Base Root Model Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Base Root Model Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseBaseRootModelElement(BaseRootModelElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Referencable Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Referencable Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseReferencableElement(ReferencableElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Component Definition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Component Definition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseComponentDefinition(ComponentDefinition object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Named Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Named Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseNamedElement(NamedElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Base Named Element</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Base Named Element</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseBaseNamedElement(BaseNamedElement object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Operation Definition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Operation Definition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseOperationDefinition(OperationDefinition object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Java Method Operation Definition</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Java Method Operation Definition</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseJavaMethodOperationDefinition(JavaMethodOperationDefinition object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseParameter(Parameter object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Base Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Base Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseBaseParameter(BaseParameter object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Base Named Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Base Named Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseBaseNamedParameter(BaseNamedParameter object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Java Method Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Java Method Parameter</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseJavaMethodParameter(JavaMethodParameter object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>Java Method Return</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>Java Method Return</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject) doSwitch(EObject)
	 * @generated
	 */
	public Object caseJavaMethodReturn(JavaMethodReturn object) {
		return null;
	}

	/**
	 * Returns the result of interpretting the object as an instance of '<em>EObject</em>'.
	 * <!-- begin-user-doc -->
	 * This implementation returns null;
	 * returning a non-null result will terminate the switch, but this is the last case anyway.
	 * <!-- end-user-doc -->
	 * @param object the target of the switch.
	 * @return the result of interpretting the object as an instance of '<em>EObject</em>'.
	 * @see #doSwitch(org.eclipse.emf.ecore.EObject)
	 * @generated
	 */
	public Object defaultCase(EObject object) {
		return null;
	}

} //OpenlSwitch
