/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.exigen.openl.model.openl;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.exigen.openl.model.openl.OpenlPackage
 * @generated
 */
public interface OpenlFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	OpenlFactory eINSTANCE = com.exigen.openl.model.openl.impl.OpenlFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Rule Set File</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rule Set File</em>'.
	 * @generated
	 */
	RuleSetFile createRuleSetFile();

	/**
	 * Returns a new object of class '<em>Rule Set</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rule Set</em>'.
	 * @generated
	 */
	RuleSet createRuleSet();

	/**
	 * Returns a new object of class '<em>Rule Set Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rule Set Parameter</em>'.
	 * @generated
	 */
	RuleSetParameter createRuleSetParameter();

	/**
	 * Returns a new object of class '<em>Rule Set Return</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rule Set Return</em>'.
	 * @generated
	 */
	RuleSetReturn createRuleSetReturn();

	/**
	 * Returns a new object of class '<em>Rule Set Composite Parameter</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Rule Set Composite Parameter</em>'.
	 * @generated
	 */
	RuleSetCompositeParameter createRuleSetCompositeParameter();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	OpenlPackage getOpenlPackage();

} //OpenlFactory
